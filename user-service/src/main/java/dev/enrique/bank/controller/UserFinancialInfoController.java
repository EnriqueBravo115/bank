package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.FINANCIAL;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.commons.dto.response.UserFinancialInfoDetailResponse;
import dev.enrique.bank.commons.dto.response.UserFinancialInfoResponse;
import dev.enrique.bank.service.UserFinancialInfoService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(FINANCIAL)
@RequiredArgsConstructor
public class UserFinancialInfoController {
    private final UserFinancialInfoService userFinancialInfoService;

    @PreAuthorize("hasRole('CUSTOMER_BASIC')")
    @GetMapping("/me")
    public ResponseEntity<UserFinancialInfoResponse> getMyFinancialInfo(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userFinancialInfoService.getFinancialInfoByKeycloakId(jwt.getSubject()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-detail/{userId}")
    public ResponseEntity<UserFinancialInfoDetailResponse> getFinancialInfoDetailByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(userFinancialInfoService.getFinancialInfoDetailByUserId(userId));
    }
}
