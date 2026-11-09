package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.LIMIT;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.commons.dto.request.LimitRequest;
import dev.enrique.bank.commons.dto.response.LimitResponse;
import dev.enrique.bank.service.TransactionLimitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Limits per account, movement type and period: set by an admin, visible to the account's owner.
@RestController
@RequestMapping(LIMIT)
@RequiredArgsConstructor
public class LimitController {
    private final TransactionLimitService transactionLimitService;

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public ResponseEntity<LimitResponse> save(@Valid @RequestBody LimitRequest request) {
        return ResponseEntity.ok(transactionLimitService.save(request));
    }

    @PreAuthorize("hasRole('CUSTOMER_BASIC')")
    @GetMapping("/me")
    public ResponseEntity<List<LimitResponse>> getMyLimits(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(transactionLimitService.findByUserId(jwt.getSubject()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{accountNumber}")
    public ResponseEntity<List<LimitResponse>> getByAccountNumber(@PathVariable String accountNumber) {
        return ResponseEntity.ok(transactionLimitService.findByAccountNumber(accountNumber));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{limitId}")
    public ResponseEntity<Void> delete(@PathVariable Long limitId) {
        transactionLimitService.delete(limitId);
        return ResponseEntity.noContent().build();
    }
}
