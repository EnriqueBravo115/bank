package dev.enrique.bank.commons.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import dev.enrique.bank.commons.enums.RegisterStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InvalidRegistrationStepException extends RuntimeException {

    public InvalidRegistrationStepException(RegisterStatus current, RegisterStatus expected) {
        super("Invalid register step, expected: " + expected + " current: " + current);
    }
}
