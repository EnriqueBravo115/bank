package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.REGISTER;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.commons.dto.request.UserFinancialInfoRequest;
import dev.enrique.bank.commons.dto.request.UserKycDataRequest;
import dev.enrique.bank.commons.dto.request.UserProfileRequest;
import dev.enrique.bank.commons.dto.request.UserRegisterRequest;
import dev.enrique.bank.commons.dto.response.UserRegisterResponse;
import dev.enrique.bank.service.RegisterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(REGISTER)
@RequiredArgsConstructor
public class RegisterController {
    private final RegisterService registerService;

    @PostMapping
    public ResponseEntity<UserRegisterResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        return ResponseEntity.ok(registerService.register(request));
    }

    @PutMapping("/me/profile")
    public ResponseEntity<Void> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserProfileRequest request) {
        registerService.updateProfile(jwt.getSubject(), request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/kyc")
    public ResponseEntity<Void> updateKycData(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserKycDataRequest request) {
        registerService.updateKycData(jwt.getSubject(), request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/financial")
    public ResponseEntity<Void> updateFinancialInfo(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserFinancialInfoRequest request) {
        registerService.updateFinancialInfo(jwt.getSubject(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/verify-email")
    public ResponseEntity<Void> resendVerificationEmail(@AuthenticationPrincipal Jwt jwt) {
        registerService.resendVerificationEmail(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
