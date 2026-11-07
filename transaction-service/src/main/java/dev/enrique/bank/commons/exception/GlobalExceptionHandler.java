package dev.enrique.bank.commons.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dev.enrique.bank.commons.dto.response.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({ TransactionNotFoundException.class, AccountNotFoundException.class })
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        return error(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(AccountNotOwnedException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotOwned(AccountNotOwnedException ex) {
        return error(HttpStatus.FORBIDDEN, ex);
    }

    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransaction(InvalidTransactionException ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(TransactionNotReversibleException.class)
    public ResponseEntity<ErrorResponse> handleNotReversible(TransactionNotReversibleException ex) {
        return error(HttpStatus.CONFLICT, ex);
    }

    // Nothing was recorded: the account could not be checked, so the request is refused.
    @ExceptionHandler(AccountServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleAccountServiceUnavailable(AccountServiceUnavailableException ex) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, ex);
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, RuntimeException ex) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), ex.getMessage(), LocalDateTime.now()));
    }
}
