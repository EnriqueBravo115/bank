package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.KYC;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.commons.dto.response.UserKycDetailResponse;
import dev.enrique.bank.commons.dto.response.UserKycResponse;
import dev.enrique.bank.commons.enums.DocumentType;
import dev.enrique.bank.commons.enums.RegisterStatus;
import dev.enrique.bank.service.UserKycService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(KYC)
@RequiredArgsConstructor
public class UserKycController {
    private final UserKycService userKycService;

    @PreAuthorize("hasRole('CUSTOMER_BASIC')")
    @GetMapping("/get-by-user-id/{userId}")
    public ResponseEntity<UserKycResponse> getKycByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(userKycService.getKycByUserId(userId));
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

    @PreAuthorize("hasRole('CUSTOMER_BASIC')")
    @GetMapping("/get-detail/{userId}")
    public ResponseEntity<UserKycDetailResponse> getKycDetailByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(userKycService.getKycDetailByUserId(userId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-by-email/{email}")
    public ResponseEntity<UserKycResponse> getKycByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userKycService.getKycByEmail(email));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-by-document-type/{documentType}")
    public ResponseEntity<List<UserKycResponse>> getKycByDocumentType(@PathVariable DocumentType documentType) {
        return ResponseEntity.ok(userKycService.getKycByDocumentType(documentType));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-all-detailed")
    public ResponseEntity<List<UserKycDetailResponse>> getAllKycDetailed() {
        return ResponseEntity.ok(userKycService.getAllKycDetailed());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-active")
    public ResponseEntity<List<UserKycResponse>> getKycFromActiveUsers() {
        return ResponseEntity.ok(userKycService.getKycFromActiveUsers());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-by-status/{status}")
    public ResponseEntity<List<UserKycDetailResponse>> getKycByRegisterStatus(@PathVariable RegisterStatus status) {
        return ResponseEntity.ok(userKycService.getKycByRegisterStatus(status));
    }

    @PreAuthorize("hasRole('CUSTOMER_BASIC')")
    @GetMapping("/exists/{userId}")
    public ResponseEntity<Boolean> existsKycByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(userKycService.existsKycByUserId(userId));
    }
}
