package dev.enrique.bank.commons.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long userId) {
        this("id", userId);
    }

    public UserNotFoundException(String field, Object value) {
        super("User not found with " + field + ": " + value);
    }
}
