package dev.enrique.bank.broker.outbox;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.enrique.bank.broker.event.CreateAccountEvent;
import dev.enrique.bank.broker.producer.AccountProducer;
import dev.enrique.bank.commons.enums.AccountType;
import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.dao.OutboxEventRepository;
import dev.enrique.bank.model.OutboxEvent;
import dev.enrique.bank.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Transactional outbox for the account-requested event. {@link #enqueue} runs inside the KYC
 * transaction, so the event exists if and only if the KYC was committed; {@link #publishPending}
 * later sends it to Kafka (at-least-once: account-service must ignore repeated events).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountRequestOutbox {
    public static final int MAX_ATTEMPTS = 10;
    private static final int BATCH_SIZE = 50;
    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final OutboxEventRepository outboxEventRepository;
    private final AccountProducer accountProducer;
    private final ObjectMapper objectMapper;

    public void enqueue(User user) {
        CreateAccountEvent event = CreateAccountEvent.builder()
                .email(user.getEmail())
                .accountType(AccountType.SAVING)
                .currency(Currency.MX)
                .build();

        outboxEventRepository.save(OutboxEvent.pending(
                user.getKeycloakId(), AccountProducer.ACCOUNT_REQUESTED_TOPIC, toJson(event)));
    }

    @Transactional
    public void publishPending() {
        for (OutboxEvent outboxEvent : outboxEventRepository.lockPending(AccountProducer.ACCOUNT_REQUESTED_TOPIC, BATCH_SIZE)) {
            try {
                CreateAccountEvent event = objectMapper.readValue(outboxEvent.getPayload(), CreateAccountEvent.class);
                // Wait for the broker ack: the row is only marked SENT once Kafka has the message.
                accountProducer.sendCreateAccountEvent(event, outboxEvent.getAggregateId(), outboxEvent.getId())
                        .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                outboxEvent.markSent();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                outboxEvent.registerFailure("interrupted", MAX_ATTEMPTS);
                return;
            } catch (Exception ex) {
                outboxEvent.registerFailure(ex.toString(), MAX_ATTEMPTS);
                log.warn("Could not publish outbox event {} (attempt {}): {}",
                        outboxEvent.getId(), outboxEvent.getAttempts(), ex.toString());
                // The broker is most likely down: stop this batch and retry on the next run.
                return;
            }
        }
    }

    private String toJson(CreateAccountEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize account-requested event", ex);
        }
    }
}
