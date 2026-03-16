package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.TRANSACTION_ANALYTICS;

import java.math.BigDecimal;
import java.time.Month;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.commons.dto.response.TransactionBasicResponse;
import dev.enrique.bank.commons.dto.response.TransactionDetailedResponse;
import dev.enrique.bank.commons.dto.response.TransactionSummaryResponse;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.commons.util.BasicMapper;
import dev.enrique.bank.service.TransactionAnalyticsService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(TRANSACTION_ANALYTICS)
@RequiredArgsConstructor
public class TransactionAnalyticsController {
    private final TransactionAnalyticsService transactionAnalyticsService;
    private final BasicMapper mapper;

    @GetMapping("/group-by-type")
    public ResponseEntity<Map<TransactionType, List<TransactionDetailedResponse>>> groupTransactionsByType(
            @RequestParam String sourceIdentifier,
            @RequestParam TransactionStatus status) {
        return ResponseEntity.ok(mapper.convertToResposeMap(
                transactionAnalyticsService.groupTransactionsByType(sourceIdentifier, status),
                TransactionDetailedResponse.class));
    }

    @GetMapping("/sum-by-type")
    public ResponseEntity<Map<TransactionType, BigDecimal>> sumTransactionsByType(
            @RequestParam String sourceIdentifier,
            @RequestParam TransactionStatus status) {
        return ResponseEntity.ok(transactionAnalyticsService.sumTransactionsByType(sourceIdentifier, status));
    }

    @GetMapping("/partition-by-amount")
    public ResponseEntity<Map<Boolean, List<TransactionBasicResponse>>> partitionTransactionsByAmount(
            @RequestParam String sourceIdentifier,
            @RequestParam TransactionStatus status,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(mapper.convertToResposeMap(
                transactionAnalyticsService.partitionTransactionsByAmount(sourceIdentifier, status, amount),
                TransactionBasicResponse.class));
    }

    @GetMapping("/type-summary")
    public ResponseEntity<Map<TransactionType, TransactionSummaryResponse>> getTransactionTypeSummary(
            @RequestParam String sourceIdentifier,
            @RequestParam TransactionStatus status) {
        return ResponseEntity.ok(transactionAnalyticsService.getTransactionTypeSummary(sourceIdentifier, status));
    }

    @GetMapping("/total-amount")
    public ResponseEntity<BigDecimal> getTotalTransactionAmount(
            @RequestParam String sourceIdentifier,
            @RequestParam TransactionStatus status,
            @RequestParam TransactionType type) {
        return ResponseEntity.ok(transactionAnalyticsService.calculateTotalAmountByStatusAndType(
                sourceIdentifier, status, type));
    }

    @GetMapping("/average-days")
    public ResponseEntity<Double> getAverageDaysBetweenTransactions(
            @RequestParam String sourceIdentifier,
            @RequestParam TransactionStatus status) {
        return ResponseEntity.ok(transactionAnalyticsService.getAverageDaysBetweenTransactions(
                sourceIdentifier, status));
    }

    // FIX: Internal Server Error
    @GetMapping("/max-by-type")
    public ResponseEntity<Map<TransactionType, List<TransactionBasicResponse>>> getMaxByTransactionType(
            @RequestParam String sourceIdentifier,
            @RequestParam TransactionStatus status) {
        return ResponseEntity.ok(mapper.convertToResposeMap(
                transactionAnalyticsService.getMaxTransactionByType(sourceIdentifier, status),
                TransactionBasicResponse.class));
    }

    @GetMapping("/count-by-month")
    public ResponseEntity<Map<Month, Long>> countTransactionsByMonth(
            @RequestParam String sourceIdentifier,
            @RequestParam TransactionStatus status) {
        return ResponseEntity.ok(transactionAnalyticsService.countTransactionsByMonth(sourceIdentifier, status));
    }

    @GetMapping("/average-amount")
    public ResponseEntity<Map<TransactionType, BigDecimal>> getAverageAmountByType(
            @RequestParam String sourceIdentifier,
            @RequestParam TransactionStatus status) {
        return ResponseEntity.ok(transactionAnalyticsService.getAverageAmountByType(sourceIdentifier, status));
    }
}
