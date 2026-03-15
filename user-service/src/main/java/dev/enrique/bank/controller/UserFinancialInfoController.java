package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.FINANCIAL;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.commons.dto.response.UserFinancialInfoDetailResponse;
import dev.enrique.bank.commons.dto.response.UserFinancialInfoResponse;
import dev.enrique.bank.commons.enums.IncomeSource;
import dev.enrique.bank.commons.enums.MaritalStatus;
import dev.enrique.bank.commons.enums.OccupationType;
import dev.enrique.bank.service.UserFinancialInfoService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(FINANCIAL)
@RequiredArgsConstructor
public class UserFinancialInfoController {
    private final UserFinancialInfoService userFinancialInfoService;

    @PreAuthorize("hasRole('CUSTOMER_BASIC')")
    @GetMapping("/get-by-user-id/{userId}")
    public ResponseEntity<UserFinancialInfoResponse> getFinancialInfoByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(userFinancialInfoService.getFinancialInfoByUserId(userId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-by-email")
    public ResponseEntity<UserFinancialInfoResponse> getFinancialInfoByEmail(
            @RequestParam String email) {
        return ResponseEntity.ok(userFinancialInfoService.getFinancialInfoByEmail(email));
    }

    @PreAuthorize("hasRole('CUSTOMER_BASIC')")
    @GetMapping("/get-detail/{userId}")
    public ResponseEntity<UserFinancialInfoDetailResponse> getFinancialInfoDetailByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(userFinancialInfoService.getFinancialInfoDetailByUserId(userId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-by-occupation/{occupationType}")
    public ResponseEntity<List<UserFinancialInfoResponse>> getFinancialInfoByOccupationType(
            @PathVariable OccupationType occupationType) {
        return ResponseEntity.ok(userFinancialInfoService.getFinancialInfoByOccupationType(occupationType));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-by-income-source/{incomeSource}")
    public ResponseEntity<List<UserFinancialInfoResponse>> getFinancialInfoByIncomeSource(
            @PathVariable IncomeSource incomeSource) {
        return ResponseEntity.ok(userFinancialInfoService.getFinancialInfoByIncomeSource(incomeSource));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-by-marital-status/{maritalStatus}")
    public ResponseEntity<List<UserFinancialInfoResponse>> getFinancialInfoByMaritalStatus(
            @PathVariable MaritalStatus maritalStatus) {
        return ResponseEntity.ok(userFinancialInfoService.getFinancialInfoByMaritalStatus(maritalStatus));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-by-income-range")
    public ResponseEntity<List<UserFinancialInfoResponse>> getFinancialInfoByIncomeRange(
            @RequestParam BigDecimal min,
            @RequestParam BigDecimal max) {
        return ResponseEntity.ok(userFinancialInfoService.getFinancialInfoByIncomeRange(min, max));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-all-detailed")
    public ResponseEntity<List<UserFinancialInfoDetailResponse>> getAllFinancialInfoDetailed() {
        return ResponseEntity.ok(userFinancialInfoService.getAllFinancialInfoDetailed());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-active")
    public ResponseEntity<List<UserFinancialInfoResponse>> getFinancialInfoFromActiveUsers() {
        return ResponseEntity.ok(userFinancialInfoService.getFinancialInfoFromActiveUsers());
    }

    @PreAuthorize("hasRole('CUSTOMER_BASIC')")
    @GetMapping("/exists/{userId}")
    public ResponseEntity<Boolean> existsFinancialInfoByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(userFinancialInfoService.existsFinancialInfoByUserId(userId));
    }
}
