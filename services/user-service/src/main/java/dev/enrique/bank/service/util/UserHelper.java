package dev.enrique.bank.service.util;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import dev.enrique.bank.commons.exception.ApiRequestException;
import dev.enrique.bank.commons.exception.InputFieldException;
import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.model.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import static dev.enrique.bank.commons.constants.ErrorMessage.*;
import static dev.enrique.bank.commons.constants.PathConstants.AUTH_USER_ID_HEADER;

@Component
@RequiredArgsConstructor
public class UserHelper {
    private final UserRepository userRepository;

    public void processInputErrors(BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new InputFieldException(bindingResult);
        }
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
}
