package dev.enrique.bank.commons.exception;

public class LimitNotFoundException extends RuntimeException {
    public LimitNotFoundException(Long id) {
        super("Limit not found with id: " + id);
    }
}
