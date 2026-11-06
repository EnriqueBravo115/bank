package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.TRANSACTION_ANALYTICS;

import java.math.BigDecimal;
import java.time.Month;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.commons.dto.response.TransactionResponse;
import dev.enrique.bank.commons.dto.response.TransactionSummaryResponse;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.service.TransactionAnalyticsService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(TRANSACTION_ANALYTICS)
@RequiredArgsConstructor
public class TransactionAnalyticsController {
    private final TransactionAnalyticsService transactionAnalyticsService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/group-by-type")
    public ResponseEntity<Map<TransactionType, List<TransactionResponse>>> groupTransactionsByType(
            @RequestParam String sourceIdentifier,
            @RequestParam TransactionStatus status) {
        return ResponseEntity.ok(transactionAnalyticsService.groupTransactionsByType(sourceIdentifier, status));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_BASIC')")
    @GetMapping("/sum-by-type")
    public ResponseEntity<Map<TransactionType, BigDecimal>> sumTransactionsByType(
            @RequestParam String sourceIdentifier,
            @RequestParam TransactionStatus status) {
        return ResponseEntity.ok(transactionAnalyticsService.sumTransactionsByType(sourceIdentifier, status));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/partition-by-amount")
    public ResponseEntity<Map<Boolean, List<TransactionResponse>>> partitionTransactionsByAmount(
            @RequestParam String sourceIdentifier,
            @RequestParam TransactionStatus status,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(
                transactionAnalyticsService.partitionTransactionsByAmount(sourceIdentifier, status, amount));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/type-summary")
    public ResponseEntity<Map<TransactionType, TransactionSummaryResponse>> getTransactionTypeSummary(
            @RequestParam String sourceIdentifier,
            @RequestParam TransactionStatus status) {
        return ResponseEntity.ok(transactionAnalyticsService.getTransactionTypeSummary(sourceIdentifier, status));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_BASIC')")
    @GetMapping("/total-amount")
    public ResponseEntity<BigDecimal> getTotalTransactionAmount(
            @RequestParam String sourceIdentifier,
            @RequestParam TransactionStatus status,
            @RequestParam TransactionType type) {
        return ResponseEntity.ok(transactionAnalyticsService.calculateTotalAmountByStatusAndType(
                sourceIdentifier, status, type));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/average-days")
    public ResponseEntity<Double> getAverageDaysBetweenTransactions(
            @RequestParam String sourceIdentifier,
            @RequestParam TransactionStatus status) {
        return ResponseEntity.ok(transactionAnalyticsService.getAverageDaysBetweenTransactions(
                sourceIdentifier, status));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/max-by-type")
    public ResponseEntity<Map<TransactionType, TransactionResponse>> getMaxByTransactionType(
            @RequestParam String sourceIdentifier,
            @RequestParam TransactionStatus status) {
        return ResponseEntity.ok(transactionAnalyticsService.getMaxTransactionByType(sourceIdentifier, status));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_BASIC')")
    @GetMapping("/count-by-month")
    public ResponseEntity<Map<Month, Long>> countTransactionsByMonth(
            @RequestParam String sourceIdentifier,
            @RequestParam TransactionStatus status) {
        return ResponseEntity.ok(transactionAnalyticsService.countTransactionsByMonth(sourceIdentifier, status));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_BASIC')")
    @GetMapping("/average-amount")
    public ResponseEntity<Map<TransactionType, BigDecimal>> getAverageAmountByType(
            @RequestParam String sourceIdentifier,
            @RequestParam TransactionStatus status) {
        return ResponseEntity.ok(transactionAnalyticsService.getAverageAmountByType(sourceIdentifier, status));
    }
}
