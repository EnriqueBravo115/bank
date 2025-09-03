package dev.enrique.bank.service.util;

import static dev.enrique.bank.constants.ErrorMessage.USER_NOT_FOUND;
import static dev.enrique.bank.constants.PathConstants.AUTH_USER_ID_HEADER;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.dto.request.RegistrationRequest;
import dev.enrique.bank.exception.ApiRequestException;
import dev.enrique.bank.exception.InputFieldException;
import dev.enrique.bank.exception.UniqueFieldValidationException;
import dev.enrique.bank.model.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserHelper {
    private final UserRepository userRepository;

    public void processInputErrors(BindingResult bindingResult) {
        if (bindingResult.hasErrors())
            throw new InputFieldException(bindingResult);
    }

    public Long getAuthenticatedUserId() {
        return getUserId();
    }

    public User getAuthenticatedUser() {
        return userRepository.findById(getUserId())
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    public void checkMatchPasswords(String password, String password2) {
        if (password == null || !password.equals(password2)) {
            processPasswordException("password", "Passwords do not match!", HttpStatus.BAD_REQUEST);
        }
    }

    public void processPasswordException(String paramName, String exceptionMessage, HttpStatus status) {
        throw new InputFieldException(status, Map.of(paramName, exceptionMessage));
    }

    public Long getUserId() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
        String userIdHeader = request.getHeader(AUTH_USER_ID_HEADER);

        if (userIdHeader == null || userIdHeader.isEmpty()) {
            throw new RuntimeException("auth user id header is missing");
        }
        return (Long) Long.parseLong(userIdHeader);
    }

    public void ensureUserIdentifierAreUnique(RegistrationRequest request) {
        userRepository.findActiveByAnyIdentifier(request.getEmail(), request.getRfc(), request.getCurp()).ifPresent(
                user -> {
                    List<String> errors = new ArrayList<>();
                    if (user.getEmail().equals(request.getEmail()))
                        errors.add("Email is already taken");

                    if (user.getRfc().equals(request.getRfc()))
                        errors.add("RFC is already taken");

                    if (user.getCurp().equals(request.getCurp()))
                        errors.add("CURP is already taken");

                    if (!errors.isEmpty()) {
                        throw new UniqueFieldValidationException(errors);
                    }
                });
    }
}
