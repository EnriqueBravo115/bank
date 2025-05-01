package dev.enrique.bank.service.impl;

import static dev.enrique.bank.commons.constants.ErrorMessage.EMAIL_NOT_FOUND;
import static dev.enrique.bank.commons.constants.ErrorMessage.INVALID_PASSWORD_RESET_CODE;
import static dev.enrique.bank.commons.constants.ErrorMessage.USER_NOT_FOUND;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import dev.enrique.bank.commons.enums.UserRole;
import dev.enrique.bank.commons.exception.ApiRequestException;
import dev.enrique.bank.commons.utils.UserServiceHelper;
import dev.enrique.bank.config.JwtProvider;
import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.dao.projection.AuthUserProjection;
import dev.enrique.bank.dao.projection.UserCommonProjection;
import dev.enrique.bank.dao.projection.UserPrincipalProjection;
import dev.enrique.bank.dto.request.AuthenticationRequest;
import dev.enrique.bank.model.User;
import dev.enrique.bank.service.AuthenticationService;
import dev.enrique.bank.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import static dev.enrique.bank.commons.constants.PathConstants.AUTH_USER_ID_HEADER;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final UserServiceHelper userServiceHelper;
    private final EmailService emailService;

    @Override
    public Long getAuthenticatedUserId() {
        return getUserId();
    }

    @Override
    public User getAuthenticatedUser() {
        return userRepository.findById(getUserId())
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    @Override
    public UserPrincipalProjection getUserPrincipalByEmail(String email) {
        return userRepository.getUserByEmail(email, UserPrincipalProjection.class)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    @Override
    public Map<String, Object> login(AuthenticationRequest request, BindingResult bindingResult) {
        userServiceHelper.processInputErrors(bindingResult);

        AuthUserProjection user = userRepository.getUserByEmail(request.getEmail(), AuthUserProjection.class)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));

        String token = jwtProvider.generateToken(UserRole.USER.name(), request.getEmail());
        return Map.of("user", user, "token", token);
    }

    @Override
    public String sendPasswordResetCode(String email, BindingResult bindingResult) {
        userServiceHelper.processInputErrors(bindingResult);
        UserCommonProjection user = userRepository.getUserByEmail(email, UserCommonProjection.class)
            .orElseThrow(() -> new ApiRequestException(EMAIL_NOT_FOUND, HttpStatus.NOT_FOUND));

        String passwordResetCode = UUID.randomUUID().toString().substring(0, 7);
        userRepository.updatePasswordResetCode(passwordResetCode, user.getId());

        String subject = "Password reset code";
        String body = "Tu código de reset es: " + passwordResetCode;
        emailService.sendEmail(email, subject, body);
        return "Password reset code send";
    }

    @Override
    public AuthUserProjection getUserByPasswordResetCode(String code) {
        return userRepository.getByPasswordResetCode(code)
            .orElseThrow(() -> new ApiRequestException(INVALID_PASSWORD_RESET_CODE, HttpStatus.BAD_REQUEST));
    }

    @Override
    public String passwordReset(String email, String password1, String password2, BindingResult bindingResult) {
        userServiceHelper.processInputErrors(bindingResult);
        return "hello";
    }

    private void checkMatchPasswords(String password1, String password2) {
        if (password1 == null || !password1.equals(password2)) {
        }
    }

    private void processPasswordException(String paramName, String exceptionMessage, HttpStatus status) {
    }

    private Long getUserId() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
        String userIdHeader = request.getHeader(AUTH_USER_ID_HEADER);

        if (userIdHeader == null || userIdHeader.isEmpty()) {
            throw new RuntimeException("auth user id header is missing");
        }
        return (Long) Long.parseLong(userIdHeader);
    }
}
