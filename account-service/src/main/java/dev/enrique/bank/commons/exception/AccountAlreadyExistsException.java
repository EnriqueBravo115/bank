package dev.enrique.bank.commons.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class AccountAlreadyExistsException extends RuntimeException {
    public AccountAlreadyExistsException(String field, String value) {
        super("An account with " + field + ": " + value + " already exists");
    }
}
