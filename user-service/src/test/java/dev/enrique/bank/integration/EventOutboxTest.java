package dev.enrique.bank.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.KafkaException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import dev.enrique.bank.broker.Topics;
import dev.enrique.bank.broker.event.CreateAccountEvent;
import dev.enrique.bank.broker.event.UserStatusChangedEvent;
import dev.enrique.bank.broker.outbox.EventOutbox;
import dev.enrique.bank.broker.producer.UserEventProducer;
import dev.enrique.bank.commons.enums.AccountType;
import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.OutboxStatus;
import dev.enrique.bank.commons.exception.OutboxEventNotFoundException;
import dev.enrique.bank.commons.exception.OutboxEventNotRetryableException;
import dev.enrique.bank.dao.OutboxEventRepository;
import dev.enrique.bank.model.OutboxEvent;
import dev.enrique.bank.util.PostgresContainerConfig;

@SpringBootTest
@ActiveProfiles("test-postgres")
class EventOutboxTest extends PostgresContainerConfig {
    @Autowired
    private EventOutbox outbox;
    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @MockitoBean
    private UserEventProducer userEventProducer;

    @BeforeEach
    void cleanUp() {
        outboxEventRepository.deleteAll();
    }

    @Test
    void shouldPublishPendingEventsAndMarkThemSent() {
        brokerAccepts();
        outbox.enqueue(Topics.ACCOUNT_REQUESTED, "kc-1", accountRequested("one@test.com"));
        outbox.enqueue(Topics.ACCOUNT_REQUESTED, "kc-2", accountRequested("two@test.com"));

        outbox.publishPending();

        assertThat(outboxEventRepository.findAll()).hasSize(2).allSatisfy(event -> {
            assertThat(event.getStatus()).isEqualTo(OutboxStatus.SENT);
            assertThat(event.getSentAt()).isNotNull();
        });
        verify(userEventProducer).send(
                eq(Topics.ACCOUNT_REQUESTED),
                eq("kc-1"),
                argThat(e -> e instanceof CreateAccountEvent created
                        && created.getEmail().equals("one@test.com")
                        && created.getAccountType() == AccountType.SAVING
                        && created.getCurrency() == Currency.MX),
                eq(eventOf("kc-1").getId()));
    }

    @Test
    void shouldPublishEventsOfDifferentTopicsInCreationOrder() {
        brokerAccepts();
        outbox.enqueue(Topics.USER_STATUS_CHANGED, "kc-1", new UserStatusChangedEvent(false));
        outbox.enqueue(Topics.USER_STATUS_CHANGED, "kc-1", new UserStatusChangedEvent(true));

        outbox.publishPending();

        InOrder order = inOrder(userEventProducer);
        order.verify(userEventProducer).send(eq(Topics.USER_STATUS_CHANGED), eq("kc-1"),
                argThat(e -> e instanceof UserStatusChangedEvent changed && !changed.isActive()), any());
        order.verify(userEventProducer).send(eq(Topics.USER_STATUS_CHANGED), eq("kc-1"),
                argThat(e -> e instanceof UserStatusChangedEvent changed && changed.isActive()), any());
    }

    @Test
    void shouldRejectEventNotRegisteredForTopic() {
        assertThatThrownBy(() -> outbox.enqueue(Topics.USER_EMAIL_CHANGED, "kc-1", new UserStatusChangedEvent(true)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(outboxEventRepository.count()).isZero();
    }

    @Test
    void shouldNotResendEventsAlreadySent() {
        brokerAccepts();
        outbox.enqueue(Topics.ACCOUNT_REQUESTED, "kc-1", accountRequested("one@test.com"));

        outbox.publishPending();
        outbox.publishPending();

        verify(userEventProducer, times(1)).send(any(), any(), any(), any());
    }

    @Test
    void shouldKeepEventPendingAndStopBatchWhenBrokerFails() {
        brokerFails();
        outbox.enqueue(Topics.ACCOUNT_REQUESTED, "kc-1", accountRequested("one@test.com"));
        outbox.enqueue(Topics.ACCOUNT_REQUESTED, "kc-2", accountRequested("two@test.com"));

        outbox.publishPending();

        OutboxEvent failed = eventOf("kc-1");
        assertThat(failed.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(failed.getAttempts()).isEqualTo(1);
        assertThat(failed.getLastError()).contains("broker down");
        assertThat(eventOf("kc-2").getAttempts()).as("batch stops at the first failure").isZero();
        verify(userEventProducer, times(1)).send(any(), any(), any(), any());
    }

    @Test
    void shouldRetryOnNextRunAfterBrokerRecovers() {
        brokerFails();
        outbox.enqueue(Topics.ACCOUNT_REQUESTED, "kc-1", accountRequested("one@test.com"));
        outbox.publishPending();

        brokerAccepts();
        outbox.publishPending(LocalDateTime.now().plusMinutes(1));

        OutboxEvent event = eventOf("kc-1");
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getLastError()).isNull();
    }

    @Test
    void shouldMarkEventFailedAfterMaxAttempts() {
        brokerFails();
        outbox.enqueue(Topics.ACCOUNT_REQUESTED, "kc-1", accountRequested("one@test.com"));

        exhaustAttempts();

        OutboxEvent event = eventOf("kc-1");
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getAttempts()).isEqualTo(EventOutbox.MAX_ATTEMPTS);
        verify(userEventProducer, times(EventOutbox.MAX_ATTEMPTS)).send(any(), any(), any(), any());
    }

    @Test
    void shouldWaitWithExponentialBackoffBetweenAttempts() {
        brokerFails();
        outbox.enqueue(Topics.ACCOUNT_REQUESTED, "kc-1", accountRequested("one@test.com"));
        // PostgreSQL keeps microseconds.
        LocalDateTime start = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);

        outbox.publishPending(start);
        assertThat(eventOf("kc-1").getNextAttemptAt()).isEqualTo(start.plusSeconds(5));

        outbox.publishPending(start.plusSeconds(4));
        verify(userEventProducer, times(1)).send(any(), any(), any(), any());

        outbox.publishPending(start.plusSeconds(5));
        verify(userEventProducer, times(2)).send(any(), any(), any(), any());
        assertThat(eventOf("kc-1").getNextAttemptAt()).isEqualTo(start.plusSeconds(15));
    }

    @Test
    void shouldHoldBackLaterEventsOfSameUserWhileEarlierOneIsInBackoff() {
        brokerFails();
        outbox.enqueue(Topics.USER_STATUS_CHANGED, "kc-1", new UserStatusChangedEvent(false));
        outbox.publishPending();
        outbox.enqueue(Topics.USER_STATUS_CHANGED, "kc-1", new UserStatusChangedEvent(true));
        outbox.enqueue(Topics.ACCOUNT_REQUESTED, "kc-2", accountRequested("two@test.com"));

        brokerAccepts();
        clearInvocations(userEventProducer);
        outbox.publishPending();

        // Other users are not blocked by kc-1's backoff, but kc-1's unblock cannot overtake its block.
        verify(userEventProducer).send(eq(Topics.ACCOUNT_REQUESTED), eq("kc-2"), any(), any());
        verify(userEventProducer, never()).send(any(), eq("kc-1"), any(), any());
    }

    @Test
    void shouldHoldBackLaterEventsOfSameUserUntilFailedOneIsRetried() {
        brokerFails();
        outbox.enqueue(Topics.USER_STATUS_CHANGED, "kc-1", new UserStatusChangedEvent(false));
        exhaustAttempts();
        outbox.enqueue(Topics.USER_STATUS_CHANGED, "kc-1", new UserStatusChangedEvent(true));

        brokerAccepts();
        clearInvocations(userEventProducer);
        outbox.publishPending(LocalDateTime.now().plusDays(30));
        verify(userEventProducer, never()).send(any(), any(), any(), any());

        OutboxEvent failed = outboxEventRepository.findAll().stream()
                .filter(event -> event.getStatus() == OutboxStatus.FAILED)
                .findFirst()
                .orElseThrow();
        outbox.retry(failed.getId());
        outbox.publishPending();

        InOrder order = inOrder(userEventProducer);
        order.verify(userEventProducer).send(eq(Topics.USER_STATUS_CHANGED), eq("kc-1"),
                argThat(e -> e instanceof UserStatusChangedEvent changed && !changed.isActive()), any());
        order.verify(userEventProducer).send(eq(Topics.USER_STATUS_CHANGED), eq("kc-1"),
                argThat(e -> e instanceof UserStatusChangedEvent changed && changed.isActive()), any());
        assertThat(outboxEventRepository.findAll()).allSatisfy(event -> {
            assertThat(event.getStatus()).isEqualTo(OutboxStatus.SENT);
            assertThat(event.getLastError()).isNull();
        });
    }

    @Test
    void shouldResetAttemptsWhenRetryingFailedEvent() {
        brokerFails();
        outbox.enqueue(Topics.ACCOUNT_REQUESTED, "kc-1", accountRequested("one@test.com"));
        exhaustAttempts();

        outbox.retry(eventOf("kc-1").getId());

        OutboxEvent event = eventOf("kc-1");
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getAttempts()).isZero();
        assertThat(event.getLastError()).as("kept until it is sent").contains("broker down");
    }

    @Test
    void shouldOnlyRetryFailedEvents() {
        outbox.enqueue(Topics.ACCOUNT_REQUESTED, "kc-1", accountRequested("one@test.com"));
        UUID pendingId = eventOf("kc-1").getId();

        assertThatThrownBy(() -> outbox.retry(pendingId)).isInstanceOf(OutboxEventNotRetryableException.class);
        assertThatThrownBy(() -> outbox.retry(UUID.randomUUID())).isInstanceOf(OutboxEventNotFoundException.class);
    }

    @Test
    void shouldPurgeOnlyOldSentEvents() {
        brokerAccepts();
        outbox.enqueue(Topics.ACCOUNT_REQUESTED, "kc-1", accountRequested("one@test.com"));
        outbox.publishPending();
        brokerFails();
        outbox.enqueue(Topics.ACCOUNT_REQUESTED, "kc-2", accountRequested("two@test.com"));
        exhaustAttempts();
        outbox.enqueue(Topics.ACCOUNT_REQUESTED, "kc-3", accountRequested("three@test.com"));

        assertThat(outbox.purgeSent(LocalDateTime.now().minusDays(7))).as("sent too recently").isZero();
        assertThat(outbox.purgeSent(LocalDateTime.now().plusSeconds(1))).isEqualTo(1);

        assertThat(outboxEventRepository.findAll())
                .extracting(OutboxEvent::getAggregateId)
                .containsExactlyInAnyOrder("kc-2", "kc-3");
    }

    // Publishes until the head event runs out of attempts, each run after its backoff has elapsed.
    private void exhaustAttempts() {
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < EventOutbox.MAX_ATTEMPTS + 2; i++) {
            outbox.publishPending(now);
            now = now.plusHours(1);
        }
    }

    private void brokerAccepts() {
        when(userEventProducer.send(any(), any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    private void brokerFails() {
        when(userEventProducer.send(any(), any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new KafkaException("broker down")));
    }

    private OutboxEvent eventOf(String keycloakId) {
        return outboxEventRepository.findAll().stream()
                .filter(event -> event.getAggregateId().equals(keycloakId))
                .findFirst()
                .orElseThrow();
    }

    private static CreateAccountEvent accountRequested(String email) {
        return CreateAccountEvent.builder().email(email).accountType(AccountType.SAVING).currency(Currency.MX).build();
    }
}
