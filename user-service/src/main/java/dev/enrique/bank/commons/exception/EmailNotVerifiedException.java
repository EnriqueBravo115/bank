package dev.enrique.bank.commons.exception;

public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException() {
        super("Email must be verified before this step");
    }
}
