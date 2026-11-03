package dev.enrique.bank.commons.exception;

public class DuplicateUserDataException extends RuntimeException {
    public DuplicateUserDataException(String field) {
        super("A user with this " + field + " is already registered");
    }
}
