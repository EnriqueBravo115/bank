package dev.enrique.bank.commons.exception;

public class InvalidRoleChangeException extends RuntimeException {
    public InvalidRoleChangeException(String message) {
        super(message);
    }
}
