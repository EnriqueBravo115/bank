package dev.enrique.bank.broker.producer;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import dev.enrique.bank.broker.event.CreateAccountEvent;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AccountProducer {
    public static final String ACCOUNT_REQUESTED_TOPIC = "user-service.account-requested";

    private final KafkaTemplate<String, CreateAccountEvent> kafkaTemplate;

    // Keyed by the user's Keycloak id so all events of a user land in the same partition, in order.
    public CompletableFuture<SendResult<String, CreateAccountEvent>> sendCreateAccountEvent(
            CreateAccountEvent event, String authId, UUID eventId) {
        ProducerRecord<String, CreateAccountEvent> record =
                new ProducerRecord<>(ACCOUNT_REQUESTED_TOPIC, authId, event);
        record.headers().add("auth-user-id", authId.getBytes(StandardCharsets.UTF_8));
        record.headers().add("event-id", eventId.toString().getBytes(StandardCharsets.UTF_8));
        return kafkaTemplate.send(record);
    }
}
