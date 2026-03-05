package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.REGISTER;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.dto.request.UserFinancialInfoRequest;
import dev.enrique.bank.dto.request.UserKycDataRequest;
import dev.enrique.bank.dto.request.UserProfileRequest;
import dev.enrique.bank.dto.request.UserRegisterRequest;
import dev.enrique.bank.dto.response.UserRegisterResponse;
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

    @PutMapping("/{userId}/profile")
    public ResponseEntity<Void> updateProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UserProfileRequest request) {
        registerService.updateProfile(userId, request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{userId}/kyc")
    public ResponseEntity<Void> updateKycData(
            @PathVariable Long userId,
            @Valid @RequestBody UserKycDataRequest request) {
        registerService.updateKycData(userId, request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{userId}/financial")
    public ResponseEntity<Void> updateFinancialInfo(
            @PathVariable Long userId,
            @Valid @RequestBody UserFinancialInfoRequest request) {
        registerService.updateFinancialInfo(userId, request);
        return ResponseEntity.noContent().build();
    }
}
