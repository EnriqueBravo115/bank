package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.*;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.dto.response.UserInfoResponse;
import dev.enrique.bank.dto.request.AuthenticationRequest;
import dev.enrique.bank.dto.request.CurrentPasswordResetRequest;
import dev.enrique.bank.dto.request.EmailRequest;
import dev.enrique.bank.dto.request.PasswordResetRequest;
import dev.enrique.bank.dto.response.AuthUserResponse;
import dev.enrique.bank.dto.response.AuthenticationResponse;
import dev.enrique.bank.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(AUTH)
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @PostMapping(LOGIN)
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody AuthenticationRequest request,
            BindingResult bindingResult) {
        return ResponseEntity.ok(authenticationService.login(request, bindingResult));
    }

    @PostMapping(FORGOT)
    public ResponseEntity<String> sendPasswordResetCode(@Valid @RequestBody EmailRequest request,
            BindingResult bindingResult) {
        return ResponseEntity.ok(authenticationService.sendPasswordResetCode(request.getEmail(), bindingResult));
    }

    @PostMapping(RESET)
    public ResponseEntity<String> passwordReset(@Valid @RequestBody PasswordResetRequest request,
            BindingResult bindingResult) {
        return ResponseEntity.ok(authenticationService.passwordReset(request.getEmail(), request.getPassword(),
                request.getPassword2(), bindingResult));
    }

    @GetMapping(RESET_CODE)
    public ResponseEntity<AuthUserResponse> getUserByPasswordResetCode(@PathVariable("code") String code) {
        return ResponseEntity.ok(authenticationService.getUserByPasswordResetCode(code));
    }

    @PostMapping(RESET_CURRENT)
    public ResponseEntity<String> currentPasswordReset(@Valid @RequestBody CurrentPasswordResetRequest request,
            BindingResult bindingResult) {
        return ResponseEntity.ok(authenticationService.currentPasswordReset(
                request.getCurrentPassword(), request.getPassword(),
                request.getPassword2(), bindingResult));
    }

    @GetMapping(USER_EMAIL)
    public UserInfoResponse getUserPrincipalByEmail(@PathVariable("email") String email) {
        return authenticationService.getUserPrincipalByEmail(email);
    }
}
