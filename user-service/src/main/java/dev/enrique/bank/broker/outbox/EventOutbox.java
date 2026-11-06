package dev.enrique.bank.broker.outbox;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.enrique.bank.broker.Topics;
import dev.enrique.bank.broker.event.CreateAccountEvent;
import dev.enrique.bank.broker.event.UserEmailChangedEvent;
import dev.enrique.bank.broker.event.UserStatusChangedEvent;
import dev.enrique.bank.broker.producer.UserEventProducer;
import dev.enrique.bank.commons.dto.response.OutboxEventResponse;
import dev.enrique.bank.commons.enums.OutboxStatus;
import dev.enrique.bank.commons.exception.OutboxEventNotFoundException;
import dev.enrique.bank.commons.exception.OutboxEventNotRetryableException;
import dev.enrique.bank.dao.OutboxEventRepository;
import dev.enrique.bank.model.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Transactional outbox. {@link #enqueue} must run inside the transaction of the business change,
 * so the event exists if and only if that change was committed; {@link #publishPending} later sends
 * it to Kafka (at-least-once: consumers must tolerate repeated events).
 *
 * A failed send is retried with exponential backoff; after {@link #MAX_ATTEMPTS} (about 40 minutes of
 * failures) the event becomes FAILED and an admin has to retry it. Until then, the later events of the
 * same user wait, so they are never delivered out of order.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventOutbox {
    public static final int MAX_ATTEMPTS = 10;
    private static final int BATCH_SIZE = 50;
    private static final long SEND_TIMEOUT_SECONDS = 10;
    static final Duration FIRST_RETRY_DELAY = Duration.ofSeconds(5);
    static final Duration MAX_RETRY_DELAY = Duration.ofMinutes(15);

    // The payload is rebuilt as its event class so JsonSerializer keeps sending the same __TypeId__ header.
    private static final Map<String, Class<?>> EVENT_TYPES = Map.of(
            Topics.ACCOUNT_REQUESTED, CreateAccountEvent.class,
            Topics.USER_STATUS_CHANGED, UserStatusChangedEvent.class,
            Topics.USER_EMAIL_CHANGED, UserEmailChangedEvent.class);

    private final OutboxEventRepository outboxEventRepository;
    private final UserEventProducer userEventProducer;
    private final ObjectMapper objectMapper;

    public void enqueue(String topic, String userId, Object event) {
        if (EVENT_TYPES.get(topic) != event.getClass()) {
            throw new IllegalArgumentException("Event " + event.getClass().getSimpleName() + " is not registered for topic " + topic);
        }
        outboxEventRepository.save(OutboxEvent.pending(userId, topic, toJson(event)));
    }

    @Transactional
    public void publishPending() {
        publishPending(LocalDateTime.now());
    }

    // Events are published in creation order and the batch stops at the first failure (the broker is
    // most likely down). lockPending holds back a user's events while an earlier one is unsent, so they
    // (e.g. block then unblock) are never delivered out of order.
    @Transactional
    public void publishPending(LocalDateTime now) {
        for (OutboxEvent outboxEvent : outboxEventRepository.lockPending(now, BATCH_SIZE)) {
            try {
                Object event = objectMapper.readValue(outboxEvent.getPayload(), EVENT_TYPES.get(outboxEvent.getTopic()));
                // Wait for the broker ack: the row is only marked SENT once Kafka has the message.
                userEventProducer.send(outboxEvent.getTopic(), outboxEvent.getAggregateId(), event, outboxEvent.getId())
                        .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                outboxEvent.markSent();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                outboxEvent.registerFailure("interrupted", MAX_ATTEMPTS, nextAttempt(outboxEvent, now));
                return;
            } catch (Exception ex) {
                outboxEvent.registerFailure(ex.toString(), MAX_ATTEMPTS, nextAttempt(outboxEvent, now));
                log.warn("Could not publish outbox event {} to {} (attempt {}): {}",
                        outboxEvent.getId(), outboxEvent.getTopic(), outboxEvent.getAttempts(), ex.toString());
                // The broker is most likely down: stop this batch and retry on the next run.
                return;
            }
        }
    }

    @Transactional(readOnly = true)
    public Page<OutboxEventResponse> findFailed(Pageable pageable) {
        return outboxEventRepository.findByStatusOrderByCreatedAt(OutboxStatus.FAILED, pageable)
                .map(OutboxEventResponse::from);
    }

    // Puts a FAILED event back in the queue with a fresh set of attempts; the user's later events,
    // held back until now, go out right after it.
    @Transactional
    public OutboxEventResponse retry(UUID id) {
        OutboxEvent event = outboxEventRepository.findById(id)
                .orElseThrow(() -> new OutboxEventNotFoundException(id));
        if (event.getStatus() != OutboxStatus.FAILED) {
            throw new OutboxEventNotRetryableException(id, event.getStatus());
        }
        event.retry(LocalDateTime.now());
        return OutboxEventResponse.from(event);
    }

    // SENT events are only kept for troubleshooting; PENDING and FAILED ones are never deleted.
    @Transactional
    public int purgeSent(LocalDateTime sentBefore) {
        return outboxEventRepository.deleteByStatusAndSentAtBefore(OutboxStatus.SENT, sentBefore);
    }

    // 5s, 10s, 20s ... capped at 15 minutes.
    private static LocalDateTime nextAttempt(OutboxEvent event, LocalDateTime now) {
        Duration delay = FIRST_RETRY_DELAY.multipliedBy(1L << Math.min(event.getAttempts(), 20));
        return now.plus(delay.compareTo(MAX_RETRY_DELAY) > 0 ? MAX_RETRY_DELAY : delay);
    }

    private String toJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize outbox event", ex);
        }
    }
}
