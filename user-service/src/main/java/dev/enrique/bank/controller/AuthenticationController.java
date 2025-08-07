package dev.enrique.bank.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.dao.projection.AuthUserProjection;
import dev.enrique.bank.dto.UserInfoResponse;
import dev.enrique.bank.dto.request.AuthenticationRequest;
import dev.enrique.bank.dto.request.CurrentPasswordResetRequest;
import dev.enrique.bank.dto.request.EmailRequest;
import dev.enrique.bank.dto.request.PasswordResetRequest;
import dev.enrique.bank.dto.response.AuthUserResponse;
import dev.enrique.bank.dto.response.AuthenticationResponse;
import dev.enrique.bank.mapper.AuthenticationMapper;
import dev.enrique.bank.mapper.BasicMapper;
import dev.enrique.bank.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final BasicMapper basicMapper;
    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody AuthenticationRequest request,
            BindingResult bindingResult) {

        Map<String, Object> authResult = authenticationService.login(request, bindingResult);

        AuthenticationResponse response = AuthenticationResponse.builder()
                .user(basicMapper.convertToResponse(authResult.get("user"), AuthUserResponse.class))
                .token((String) authResult.get("token"))
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot")
    public ResponseEntity<String> sendPasswordResetCode(@Valid @RequestBody EmailRequest request,
            BindingResult bindingResult) {
        return ResponseEntity.ok(authenticationService.sendPasswordResetCode(request.getEmail(), bindingResult));
    }

    @GetMapping("/reset/{code}")
    public ResponseEntity<AuthUserResponse> getUserByPasswordResetCode(@PathVariable("code") String code) {
        AuthUserProjection authUserProjection = authenticationService.getUserByPasswordResetCode(code);
        return ResponseEntity.ok(basicMapper.convertToResponse(authUserProjection, AuthUserResponse.class));
    }

    @PostMapping("/reset")
    public ResponseEntity<String> passwordReset(@Valid @RequestBody PasswordResetRequest request,
            BindingResult bindingResult) {
        return ResponseEntity.ok(authenticationService.passwordReset(
                request.getEmail(), request.getPassword(),
                request.getPassword2(), bindingResult));
    }

    @PostMapping("/reset/current")
    public ResponseEntity<String> currentPasswordReset(@Valid @RequestBody CurrentPasswordResetRequest request,
            BindingResult bindingResult) {
        return ResponseEntity.ok(authenticationService.currentPasswordReset(
                request.getCurrentPassword(), request.getPassword(),
                request.getPassword2(), bindingResult));
    }

    @GetMapping("/user/{email}")
    public UserInfoResponse getUserPrincipalByEmail(@PathVariable("email") String email) {
        return basicMapper.convertToResponse(authenticationService.getUserPrincipalByEmail(email),
                UserInfoResponse.class);
    }
}
