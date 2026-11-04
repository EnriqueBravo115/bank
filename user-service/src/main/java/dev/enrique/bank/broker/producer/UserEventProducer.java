package dev.enrique.bank.broker.producer;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Keyed by the user's Keycloak id so all events of a user land in the same partition, in order.
    public CompletableFuture<SendResult<String, Object>> send(String topic, String userId, Object event, UUID eventId) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, userId, event);
        record.headers().add("auth-user-id", userId.getBytes(StandardCharsets.UTF_8));
        record.headers().add("event-id", eventId.toString().getBytes(StandardCharsets.UTF_8));
        return kafkaTemplate.send(record);
    }
}
