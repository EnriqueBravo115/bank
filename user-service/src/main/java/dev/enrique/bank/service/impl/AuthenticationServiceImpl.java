package dev.enrique.bank.service.impl;

import static dev.enrique.bank.constants.ErrorMessage.EMAIL_NOT_FOUND;
import static dev.enrique.bank.constants.ErrorMessage.INVALID_PASSWORD_RESET_CODE;
import static dev.enrique.bank.constants.ErrorMessage.USER_NOT_FOUND;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import dev.enrique.bank.enums.UserRole;
import dev.enrique.bank.exception.ApiRequestException;
import dev.enrique.bank.exception.InputFieldException;
import dev.enrique.bank.configuration.JwtProvider;
import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.dao.projection.AuthUserProjection;
import dev.enrique.bank.dao.projection.UserCommonProjection;
import dev.enrique.bank.dao.projection.UserPrincipalProjection;
import dev.enrique.bank.dto.request.AuthenticationRequest;
import dev.enrique.bank.service.AuthenticationService;
import dev.enrique.bank.service.EmailService;
import dev.enrique.bank.service.util.UserHelper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final UserHelper userHelper;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Override
    public UserPrincipalProjection getUserPrincipalByEmail(String email) {
        return userRepository.getUserByEmail(email, UserPrincipalProjection.class)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    @Override
    public Map<String, Object> login(AuthenticationRequest request, BindingResult bindingResult) {
        userHelper.processInputErrors(bindingResult);

        AuthUserProjection authUserProjection = userRepository
                .getUserByEmail(request.getEmail(), AuthUserProjection.class)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));

        String userPassword = userRepository.getUserPasswordById(authUserProjection.getId());

        if (!passwordEncoder.matches(request.getPassword(), userPassword)) {
            userHelper.processPasswordException("currentPassword", "The password is incorrect", HttpStatus.NOT_FOUND);
        }

        String token = jwtProvider.generateToken(UserRole.USER.name(), request.getEmail());
        return Map.of("user", authUserProjection, "token", token);
    }

    @Override
    @Transactional
    public String sendPasswordResetCode(String email, BindingResult bindingResult) {
        userHelper.processInputErrors(bindingResult);
        UserCommonProjection user = userRepository.getUserByEmail(email, UserCommonProjection.class)
                .orElseThrow(() -> new ApiRequestException(EMAIL_NOT_FOUND, HttpStatus.NOT_FOUND));

        String passwordResetCode = UUID.randomUUID().toString().substring(0, 7);
        userRepository.updatePasswordResetCode(passwordResetCode, user.getId());

        String subject = "Password reset code";
        String body = "Your reset code is:" + passwordResetCode;
        emailService.sendEmail(email, subject, body);
        return "Password reset code send";
    }

    @Override
    public AuthUserProjection getUserByPasswordResetCode(String code) {
        return userRepository.getByPasswordResetCode(code)
                .orElseThrow(() -> new ApiRequestException(INVALID_PASSWORD_RESET_CODE, HttpStatus.BAD_REQUEST));
    }

    @Override
    @Transactional
    public String passwordReset(String email, String password, String password2, BindingResult bindingResult) {
        userHelper.processInputErrors(bindingResult);
        userHelper.checkMatchPasswords(password, password2);

        UserCommonProjection user = userRepository.getUserByEmail(email, UserCommonProjection.class)
                .orElseThrow(() -> new InputFieldException(HttpStatus.NOT_FOUND, Map.of("email", EMAIL_NOT_FOUND)));

        userRepository.updatePassword(passwordEncoder.encode(password), user.getId());
        userRepository.updatePasswordResetCode(null, user.getId());
        return "Password successfully changed!";
    }

    @Override
    @Transactional
    public String currentPasswordReset(String currentPassword, String password, String password2,
            BindingResult bindingResult) {
        userHelper.processInputErrors(bindingResult);
        Long authUserId = userHelper.getAuthenticatedUserId();
        String userPassword = userRepository.getUserPasswordById(authUserId);

        if (!passwordEncoder.matches(currentPassword, userPassword)) {
            userHelper.processPasswordException("currentPassword", "The password is incorrect", HttpStatus.NOT_FOUND);
        }

        userHelper.checkMatchPasswords(password, password2);
        userRepository.updatePassword(passwordEncoder.encode(password), authUserId);
        return "Your password has been successfully updated.";
    }
}
