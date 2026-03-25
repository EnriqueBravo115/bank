package dev.enrique.bank.commons.exception;

public class TransactionLimitExceededException extends RuntimeException {
    public TransactionLimitExceededException(String reason) {
        super(reason);
    }
}
