package dev.enrique.bank.service.impl;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import dev.enrique.bank.commons.enums.UserRole;
import dev.enrique.bank.commons.exception.ApiRequestException;
//import dev.enrique.bank.exception.InputFieldException;
import dev.enrique.bank.service.util.BasicMapper;
import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.dao.projection.AuthUserProjection;
import dev.enrique.bank.dao.projection.UserCommonProjection;
import dev.enrique.bank.dao.projection.UserPrincipalProjection;
import dev.enrique.bank.dto.response.UserInfoResponse;
import dev.enrique.bank.dto.request.AuthenticationRequest;
import dev.enrique.bank.dto.response.AuthUserResponse;
import dev.enrique.bank.dto.response.AuthenticationResponse;
import dev.enrique.bank.service.AuthenticationService;
import dev.enrique.bank.service.EmailService;
import dev.enrique.bank.service.util.UserHelper;
import lombok.RequiredArgsConstructor;

// TODO: Refactor for integration with KEYCLOAK
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final UserHelper userHelper;
    private final BasicMapper basicMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Override
    public AuthenticationResponse login(AuthenticationRequest request, BindingResult bindingResult) {
        userHelper.processInputErrors(bindingResult);

        AuthUserProjection authUserProjection = userRepository
                .getUserByEmail(request.getEmail(), AuthUserProjection.class)
                .orElseThrow(() -> new ApiRequestException("User not found", HttpStatus.NOT_FOUND));

        String userPassword = userRepository.getUserPasswordById(authUserProjection.getId());
        if (!passwordEncoder.matches(request.getPassword(), userPassword)) {
            userHelper.processPasswordException("currentPassword", "The password is incorrect", HttpStatus.NOT_FOUND);
        }

        // String token = jwtProvider.generateToken(UserRole.CUSTOMER_BASIC.name(),
        // request.getEmail());
        // Map<String, Object> authResult = Map.of("user", authUserProjection, "token",
        // token);

        // return AuthenticationResponse.builder()
        // .user(basicMapper.convertToResponse(authResult.get("user"),
        // AuthUserResponse.class))
        // .token((String) authResult.get("token"))
        // .build();
        throw new UnsupportedOperationException("Unimplemented method 'updatePhone'");
    }

    @Override
    @Transactional
    public String sendPasswordResetCode(String email, BindingResult bindingResult) {
        userHelper.processInputErrors(bindingResult);
        UserCommonProjection user = userRepository.getUserByEmail(email, UserCommonProjection.class)
                .orElseThrow(() -> new ApiRequestException("Email not found", HttpStatus.NOT_FOUND));

        String passwordResetCode = UUID.randomUUID().toString().substring(0, 7);
        userRepository.updatePasswordResetCode(passwordResetCode, user.getId());

        String subject = "Password reset code";
        String body = "Your reset code is:" + passwordResetCode;
        emailService.sendEmail(email, subject, body);

        return "Password reset code send";
    }

    @Override
    public AuthUserResponse getUserByPasswordResetCode(String code) {
        AuthUserProjection authUserProjection = userRepository.getByPasswordResetCode(code)
                .orElseThrow(() -> new ApiRequestException("Password reset code is invalid", HttpStatus.BAD_REQUEST));
        return basicMapper.convertToResponse(authUserProjection, AuthUserResponse.class);
    }

    @Override
    @Transactional
    public String passwordReset(String email, String password, String password2, BindingResult bindingResult) {
        userHelper.processInputErrors(bindingResult);
        userHelper.checkMatchPasswords(password, password2);

        // UserCommonProjection user = userRepository.getUserByEmail(email,
        // UserCommonProjection.class)
        // .orElseThrow(() -> new InputFieldException(HttpStatus.NOT_FOUND,
        // Map.of("email", EMAIL_NOT_FOUND)));

        // userRepository.updatePassword(passwordEncoder.encode(password),
        // user.getId());
        // userRepository.updatePasswordResetCode(null, user.getId());
        // return "Password successfully changed!";

        throw new UnsupportedOperationException("Unimplemented method 'updatePhone'");
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

    @Override
    public UserInfoResponse getUserPrincipalByEmail(String email) {
        UserPrincipalProjection userPrincipalProjection = userRepository
                .getUserByEmail(email, UserPrincipalProjection.class)
                .orElseThrow(() -> new ApiRequestException("User not found", HttpStatus.NOT_FOUND));

        return basicMapper.convertToResponse(userPrincipalProjection, UserInfoResponse.class);
    }
}
