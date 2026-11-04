package dev.enrique.bank.model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import dev.enrique.bank.commons.enums.OutboxStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "outbox_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {
    private static final int MAX_ERROR_LENGTH = 500;

    @Id
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(name = "topic", nullable = false)
    private String topic;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OutboxStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_error", length = MAX_ERROR_LENGTH)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    public static OutboxEvent pending(String aggregateId, String topic, String payload) {
        OutboxEvent event = new OutboxEvent();
        event.id = UUID.randomUUID();
        event.aggregateId = aggregateId;
        event.topic = topic;
        event.payload = payload;
        event.status = OutboxStatus.PENDING;
        event.createdAt = LocalDateTime.now();
        return event;
    }

    public void markSent() {
        status = OutboxStatus.SENT;
        sentAt = LocalDateTime.now();
        lastError = null;
    }

    public void registerFailure(String error, int maxAttempts) {
        attempts++;
        lastError = error == null ? null : error.substring(0, Math.min(error.length(), MAX_ERROR_LENGTH));
        if (attempts >= maxAttempts) {
            status = OutboxStatus.FAILED;
        }
    }
}
