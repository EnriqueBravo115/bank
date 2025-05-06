package dev.enrique.bank.commons.exception;

public class ScheduledTransferNotFoundException extends RuntimeException {
    public ScheduledTransferNotFoundException(String message) {
        super(message);
    }
}
