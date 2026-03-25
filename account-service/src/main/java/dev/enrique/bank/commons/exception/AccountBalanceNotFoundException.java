package dev.enrique.bank.commons.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class AccountBalanceNotFoundException extends RuntimeException {
    public AccountBalanceNotFoundException(String accountNumber) {
        super("Account balance not found for account: " + accountNumber);
    }
}
