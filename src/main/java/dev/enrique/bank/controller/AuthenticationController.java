package dev.enrique.bank.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.dto.request.AuthenticationRequest;
import dev.enrique.bank.dto.request.CurrentPasswordResetRequest;
import dev.enrique.bank.dto.request.EmailRequest;
import dev.enrique.bank.dto.request.PasswordResetRequest;
import dev.enrique.bank.dto.response.AuthUserResponse;
import dev.enrique.bank.dto.response.AuthenticationResponse;
import dev.enrique.bank.mapper.AuthenticationMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationMapper authenticationMapper;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody AuthenticationRequest request,
            BindingResult bindingResult) {
        return ResponseEntity.ok(authenticationMapper.login(request, bindingResult));
    }

    @PostMapping("/forgot")
    public ResponseEntity<String> sendPasswordResetCode(@Valid @RequestBody EmailRequest request,
            BindingResult bindingResult) {
        return ResponseEntity.ok(authenticationMapper.sendPasswordResetCode(request.getEmail(), bindingResult));
    }

    @GetMapping("/reset/{code}")
    public ResponseEntity<AuthUserResponse> getUserByPasswordResetCode(@PathVariable("code") String code) {
        return ResponseEntity.ok(authenticationMapper.getUserByPasswordResetCode(code));
    }

    @PostMapping("/reset")
    public ResponseEntity<String> passwordReset(@Valid @RequestBody PasswordResetRequest request,
            BindingResult bindingResult) {
        return ResponseEntity.ok(authenticationMapper.passwordReset(request, bindingResult));
    }

    @PostMapping("/reset/current")
    public ResponseEntity<String> currentPasswordReset(@Valid @RequestBody CurrentPasswordResetRequest request,
            BindingResult bindingResult) {
        return ResponseEntity.ok(authenticationMapper.currentPasswordReset(request, bindingResult));
    }
}
