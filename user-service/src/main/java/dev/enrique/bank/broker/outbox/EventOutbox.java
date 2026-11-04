package dev.enrique.bank.broker.outbox;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.enrique.bank.broker.Topics;
import dev.enrique.bank.broker.event.CreateAccountEvent;
import dev.enrique.bank.broker.event.UserEmailChangedEvent;
import dev.enrique.bank.broker.event.UserStatusChangedEvent;
import dev.enrique.bank.broker.producer.UserEventProducer;
import dev.enrique.bank.dao.OutboxEventRepository;
import dev.enrique.bank.model.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Transactional outbox. {@link #enqueue} must run inside the transaction of the business change,
 * so the event exists if and only if that change was committed; {@link #publishPending} later sends
 * it to Kafka (at-least-once: consumers must tolerate repeated events).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventOutbox {
    public static final int MAX_ATTEMPTS = 10;
    private static final int BATCH_SIZE = 50;
    private static final long SEND_TIMEOUT_SECONDS = 10;

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

    // Events are published in creation order and the batch stops at the first failure,
    // so a user's events (e.g. block then unblock) are never delivered out of order.
    @Transactional
    public void publishPending() {
        for (OutboxEvent outboxEvent : outboxEventRepository.lockPending(BATCH_SIZE)) {
            try {
                Object event = objectMapper.readValue(outboxEvent.getPayload(), EVENT_TYPES.get(outboxEvent.getTopic()));
                // Wait for the broker ack: the row is only marked SENT once Kafka has the message.
                userEventProducer.send(outboxEvent.getTopic(), outboxEvent.getAggregateId(), event, outboxEvent.getId())
                        .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                outboxEvent.markSent();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                outboxEvent.registerFailure("interrupted", MAX_ATTEMPTS);
                return;
            } catch (Exception ex) {
                outboxEvent.registerFailure(ex.toString(), MAX_ATTEMPTS);
                log.warn("Could not publish outbox event {} to {} (attempt {}): {}",
                        outboxEvent.getId(), outboxEvent.getTopic(), outboxEvent.getAttempts(), ex.toString());
                // The broker is most likely down: stop this batch and retry on the next run.
                return;
            }
        }
    }

    private String toJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize outbox event", ex);
        }
    }
}
