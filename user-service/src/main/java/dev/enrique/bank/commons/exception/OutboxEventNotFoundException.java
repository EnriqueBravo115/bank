package dev.enrique.bank.commons.exception;

import java.util.UUID;

public class OutboxEventNotFoundException extends RuntimeException {
    public OutboxEventNotFoundException(UUID id) {
        super("Outbox event not found with id: " + id);
    }
}
