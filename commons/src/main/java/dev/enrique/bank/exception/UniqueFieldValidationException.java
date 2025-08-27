package dev.enrique.bank.exception;

import java.util.List;

import lombok.Getter;

@Getter
public class UniqueFieldValidationException extends RuntimeException {
    private final List<String> errors;

    public UniqueFieldValidationException(List<String> errors) {
        super("Validation failed");
        this.errors = errors;
    }
}
