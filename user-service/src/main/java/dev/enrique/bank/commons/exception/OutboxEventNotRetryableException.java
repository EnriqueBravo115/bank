package dev.enrique.bank.commons.exception;

import java.util.UUID;

import dev.enrique.bank.commons.enums.OutboxStatus;

public class OutboxEventNotRetryableException extends RuntimeException {
    public OutboxEventNotRetryableException(UUID id, OutboxStatus status) {
        super("Only FAILED events can be retried, event " + id + " is " + status);
    }
}
