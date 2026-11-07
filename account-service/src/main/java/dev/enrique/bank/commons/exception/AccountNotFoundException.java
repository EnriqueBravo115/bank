package dev.enrique.bank.commons.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String accountNumber) {
        this("number", accountNumber);
    }

    public AccountNotFoundException(String field, String value) {
        super("Account not found with " + field + ": " + value);
    }
}
