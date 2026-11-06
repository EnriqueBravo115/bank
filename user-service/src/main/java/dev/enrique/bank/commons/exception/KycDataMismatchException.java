package dev.enrique.bank.commons.exception;

public class KycDataMismatchException extends RuntimeException {
    public KycDataMismatchException(String message) {
        super(message);
    }
}
