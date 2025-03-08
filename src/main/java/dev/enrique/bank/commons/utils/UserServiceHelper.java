package dev.enrique.bank.commons.utils;

import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;

import dev.enrique.bank.commons.exception.InputFieldException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserServiceHelper {
    public void processInputErrors(BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new InputFieldException(bindingResult);
        }
    }
}
