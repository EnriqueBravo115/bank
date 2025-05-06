package dev.enrique.bank.commons.exception;

public class TransferException extends RuntimeException {
    public TransferException(String message) {
        super(message);
    }
}
