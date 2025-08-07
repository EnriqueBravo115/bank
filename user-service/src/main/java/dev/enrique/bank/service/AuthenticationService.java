package dev.enrique.bank.service;

import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import dev.enrique.bank.dto.UserInfoResponse;
import dev.enrique.bank.dto.request.AuthenticationRequest;
import dev.enrique.bank.dto.response.AuthUserResponse;
import dev.enrique.bank.dto.response.AuthenticationResponse;

@Service
public interface AuthenticationService {
    AuthenticationResponse login(AuthenticationRequest request, BindingResult bindingResult);

    String sendPasswordResetCode(String email, BindingResult bindingResult);

    AuthUserResponse getUserByPasswordResetCode(String code);

    String passwordReset(String email, String password1, String password2, BindingResult bindingResult);

    String currentPasswordReset(String currentPassword, String password, String password2, BindingResult bindingResult);

    UserInfoResponse getUserPrincipalByEmail(String email);
}
