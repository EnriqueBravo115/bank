package dev.enrique.bank.commons.exception;

/** The card (or the account's cards) are not in a state that allows the operation (409). */
public class CardStateException extends RuntimeException {
    public CardStateException(String message) {
        super(message);
    }
}
