package dev.enrique.bank.commons.exception;

public class AccountNotOwnedException extends RuntimeException {
    public AccountNotOwnedException(String message) {
        super(message);
    }
}
