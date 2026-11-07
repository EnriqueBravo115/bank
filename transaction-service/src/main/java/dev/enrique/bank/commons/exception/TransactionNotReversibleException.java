package dev.enrique.bank.commons.exception;

public class TransactionNotReversibleException extends RuntimeException {
    public TransactionNotReversibleException(String message) {
        super(message);
    }
}
