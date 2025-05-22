package dev.enrique.bank.service.util;

import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;

import dev.enrique.bank.commons.exception.InputFieldException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserHelper {
    public void processInputErrors(BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new InputFieldException(bindingResult);
        }
    }
}
