package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.KYC;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.commons.dto.response.UserKycDetailResponse;
import dev.enrique.bank.commons.dto.response.UserKycResponse;
import dev.enrique.bank.service.UserKycService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(KYC)
@RequiredArgsConstructor
public class UserKycController {
    private final UserKycService userKycService;

    @PreAuthorize("hasRole('CUSTOMER_BASIC')")
    @GetMapping("/me")
    public ResponseEntity<UserKycResponse> getMyKyc(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userKycService.getKycByKeycloakId(jwt.getSubject()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-detail/{userId}")
    public ResponseEntity<UserKycDetailResponse> getKycDetailByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(userKycService.getKycDetailByUserId(userId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-by-rfc/{rfc}")
    public ResponseEntity<UserKycResponse> getKycByRfc(@PathVariable String rfc) {
        return ResponseEntity.ok(userKycService.getKycByRfc(rfc));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-by-curp/{curp}")
    public ResponseEntity<UserKycResponse> getKycByCurp(@PathVariable String curp) {
        return ResponseEntity.ok(userKycService.getKycByCurp(curp));
    }
}
