package dev.enrique.bank.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import dev.enrique.bank.dao.projection.AuthUserProjection;
import dev.enrique.bank.dao.projection.UserPrincipalProjection;
import dev.enrique.bank.dto.request.AuthenticationRequest;

@Service
public interface AuthenticationService {
    UserPrincipalProjection getUserPrincipalByEmail(String email);

    Map<String, Object> login(AuthenticationRequest request, BindingResult bindingResult);

    String sendPasswordResetCode(String email, BindingResult bindingResult);

    AuthUserProjection getUserByPasswordResetCode(String code);

    String passwordReset(String email, String password1, String password2, BindingResult bindingResult);

    String currentPasswordReset(String currentPassword, String password, String password2, BindingResult bindingResult);
}
