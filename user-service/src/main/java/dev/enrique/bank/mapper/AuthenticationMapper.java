package dev.enrique.bank.mapper;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;

import dev.enrique.bank.dao.projection.AuthUserProjection;
import dev.enrique.bank.dto.request.AuthenticationRequest;
import dev.enrique.bank.dto.request.CurrentPasswordResetRequest;
import dev.enrique.bank.dto.request.PasswordResetRequest;
import dev.enrique.bank.dto.response.AuthUserResponse;
import dev.enrique.bank.dto.response.AuthenticationResponse;
import dev.enrique.bank.service.AuthenticationService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthenticationMapper {
    private final BasicMapper basicMapper;
    private final AuthenticationService authenticationService;

    public AuthenticationResponse login(AuthenticationRequest request, BindingResult bindingResult) {
        Map<String, Object> authResult = authenticationService.login(request, bindingResult);
        AuthenticationResponse response = new AuthenticationResponse();

        response.setUser(basicMapper.convertToResponse(authResult.get("user"), AuthUserResponse.class));
        response.setToken((String) authResult.get("token"));
        return response;
    }

    public String sendPasswordResetCode(String email, BindingResult bindingResult) {
        return authenticationService.sendPasswordResetCode(email, bindingResult);
    }

    public AuthUserResponse getUserByPasswordResetCode(String code) {
        AuthUserProjection authUserProjection = authenticationService.getUserByPasswordResetCode(code);
        return basicMapper.convertToResponse(authUserProjection, AuthUserResponse.class);
    }

    public String passwordReset(PasswordResetRequest request, BindingResult bindingResult) {
        return authenticationService.passwordReset(request.getEmail(), request.getPassword(), request.getPassword2(),
                bindingResult);
    }

    public String currentPasswordReset(CurrentPasswordResetRequest request, BindingResult bindingResult) {
        return authenticationService.currentPasswordReset(request.getCurrentPassword(), request.getPassword(),
                request.getPassword2(), bindingResult);
    }
}
