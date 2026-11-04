package dev.enrique.bank.commons.exception;

public class EmailDeliveryException extends RuntimeException {
    public EmailDeliveryException(Throwable cause) {
        super("Could not send the verification email, try again later", cause);
    }
}
