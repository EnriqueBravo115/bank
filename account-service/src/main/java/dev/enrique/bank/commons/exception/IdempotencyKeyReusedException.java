package dev.enrique.bank.commons.exception;

/** The Idempotency-Key was already used for a movement with other data (422). */
public class IdempotencyKeyReusedException extends RuntimeException {
    public IdempotencyKeyReusedException(String idempotencyKey) {
        super("Idempotency-Key " + idempotencyKey + " was already used for a different movement");
    }
}
