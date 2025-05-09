package dev.enrique.bank.commons.exception;

public class TransferException extends RuntimeException {
    public TransferException(String message) {
        super(message);
    }

    public TransferException(String message, Exception e) {
        super(message, e);
    }

}
