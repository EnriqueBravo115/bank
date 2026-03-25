package dev.enrique.bank.commons.exception;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String accountNumber, BigDecimal amount) {
        super("Insufficient funds in account " + accountNumber + " to process transaction of " + amount);
    }
}
