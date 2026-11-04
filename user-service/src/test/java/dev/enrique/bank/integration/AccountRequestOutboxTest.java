package dev.enrique.bank.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.KafkaException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import dev.enrique.bank.broker.outbox.AccountRequestOutbox;
import dev.enrique.bank.broker.producer.AccountProducer;
import dev.enrique.bank.commons.enums.AccountType;
import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.OutboxStatus;
import dev.enrique.bank.dao.OutboxEventRepository;
import dev.enrique.bank.model.OutboxEvent;
import dev.enrique.bank.model.User;
import dev.enrique.bank.util.PostgresContainerConfig;

@SpringBootTest
@ActiveProfiles("test-postgres")
class AccountRequestOutboxTest extends PostgresContainerConfig {
    @Autowired
    private AccountRequestOutbox outbox;
    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @MockitoBean
    private AccountProducer accountProducer;

    @BeforeEach
    void cleanUp() {
        outboxEventRepository.deleteAll();
    }

    @Test
    void shouldPublishPendingEventsAndMarkThemSent() {
        brokerAccepts();
        outbox.enqueue(user("kc-1", "one@test.com"));
        outbox.enqueue(user("kc-2", "two@test.com"));

        outbox.publishPending();

        assertThat(outboxEventRepository.findAll()).hasSize(2).allSatisfy(event -> {
            assertThat(event.getStatus()).isEqualTo(OutboxStatus.SENT);
            assertThat(event.getSentAt()).isNotNull();
        });
        OutboxEvent first = eventOf("kc-1");
        verify(accountProducer).sendCreateAccountEvent(
                argThat(e -> e.getEmail().equals("one@test.com")
                        && e.getAccountType() == AccountType.SAVING
                        && e.getCurrency() == Currency.MX),
                eq("kc-1"),
                eq(first.getId()));
    }

    @Test
    void shouldNotResendEventsAlreadySent() {
        brokerAccepts();
        outbox.enqueue(user("kc-1", "one@test.com"));

        outbox.publishPending();
        outbox.publishPending();

        verify(accountProducer, times(1)).sendCreateAccountEvent(any(), any(), any());
    }

    @Test
    void shouldKeepEventPendingAndStopBatchWhenBrokerFails() {
        brokerFails();
        outbox.enqueue(user("kc-1", "one@test.com"));
        outbox.enqueue(user("kc-2", "two@test.com"));

        outbox.publishPending();

        OutboxEvent failed = eventOf("kc-1");
        assertThat(failed.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(failed.getAttempts()).isEqualTo(1);
        assertThat(failed.getLastError()).contains("broker down");
        assertThat(eventOf("kc-2").getAttempts()).as("batch stops at the first failure").isZero();
        verify(accountProducer, times(1)).sendCreateAccountEvent(any(), any(), any());
    }

    @Test
    void shouldRetryOnNextRunAfterBrokerRecovers() {
        brokerFails();
        outbox.enqueue(user("kc-1", "one@test.com"));
        outbox.publishPending();

        brokerAccepts();
        outbox.publishPending();

        OutboxEvent event = eventOf("kc-1");
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getLastError()).isNull();
    }

    @Test
    void shouldMarkEventFailedAfterMaxAttempts() {
        brokerFails();
        outbox.enqueue(user("kc-1", "one@test.com"));

        for (int i = 0; i < AccountRequestOutbox.MAX_ATTEMPTS + 2; i++) {
            outbox.publishPending();
        }

        OutboxEvent event = eventOf("kc-1");
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getAttempts()).isEqualTo(AccountRequestOutbox.MAX_ATTEMPTS);
        verify(accountProducer, times(AccountRequestOutbox.MAX_ATTEMPTS)).sendCreateAccountEvent(any(), any(), any());
    }

    private void brokerAccepts() {
        when(accountProducer.sendCreateAccountEvent(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    private void brokerFails() {
        when(accountProducer.sendCreateAccountEvent(any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new KafkaException("broker down")));
    }

    private OutboxEvent eventOf(String keycloakId) {
        return outboxEventRepository.findAll().stream()
                .filter(event -> event.getAggregateId().equals(keycloakId))
                .findFirst()
                .orElseThrow();
    }

    private static User user(String keycloakId, String email) {
        User user = new User();
        user.setKeycloakId(keycloakId);
        user.setEmail(email);
        return user;
    }
}
