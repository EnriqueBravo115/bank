package dev.enrique.bank.commons.exception;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class InputFieldExceptionHandler {
    @ExceptionHandler(InputFieldException.class)
    public ResponseEntity<Map<String, String>> handleInputFieldException(InputFieldException exception) {
        InputFieldException inputFieldException;

        if (exception.getBindingResult() != null) {
            inputFieldException = new InputFieldException(exception.getBindingResult());
        } else {
            inputFieldException = new InputFieldException(exception.getHttpStatus(), exception.getErrorsMap());
        }
        return ResponseEntity.status(inputFieldException.getHttpStatus()).body(inputFieldException.getErrorsMap());
    }
}
