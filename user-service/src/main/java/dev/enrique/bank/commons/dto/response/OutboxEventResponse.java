package dev.enrique.bank.commons.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import dev.enrique.bank.commons.enums.OutboxStatus;
import dev.enrique.bank.model.OutboxEvent;

public record OutboxEventResponse(
        UUID id,
        String aggregateId,
        String topic,
        String payload,
        OutboxStatus status,
        int attempts,
        String lastError,
        LocalDateTime createdAt,
        LocalDateTime nextAttemptAt) {

    public static OutboxEventResponse from(OutboxEvent event) {
        return new OutboxEventResponse(event.getId(), event.getAggregateId(), event.getTopic(), event.getPayload(),
                event.getStatus(), event.getAttempts(), event.getLastError(), event.getCreatedAt(),
                event.getNextAttemptAt());
    }
}
