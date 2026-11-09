package dev.enrique.bank.commons.exception;

/** A purchase whose card cannot pay (422): transaction-service records it as DECLINED with this reason. */
public class CardDeclinedException extends RuntimeException {
    public CardDeclinedException(String reason) {
        super("Card declined: " + reason);
    }
}
