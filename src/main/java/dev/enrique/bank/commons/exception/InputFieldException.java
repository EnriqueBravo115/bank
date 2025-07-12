package dev.enrique.bank.commons.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InputFieldException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final Map<String, String> errorsMap;
    private BindingResult bindingResult;

    public InputFieldException(BindingResult bindingResult) {
        this.httpStatus = HttpStatus.BAD_REQUEST;
        this.errorsMap = handleErrors(bindingResult);
        this.bindingResult = bindingResult;
    }

    public InputFieldException(HttpStatus status, Map<String, String> errorsMap) {
        this.httpStatus = status;
        this.errorsMap = errorsMap;
    }

    private Map<String, String> handleErrors(BindingResult bindingResult) {
        Map<String, String> errors = new HashMap<>();
        bindingResult.getFieldErrors()
                .forEach(fieldError -> errors.put(fieldError.getField(), fieldError.getDefaultMessage()));
        return errors;
    }
}
