package dev.enrique.bank.commons.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class TransactionReversalException extends RuntimeException {

    public TransactionReversalException(String message) {
        super(message);
    }
}
