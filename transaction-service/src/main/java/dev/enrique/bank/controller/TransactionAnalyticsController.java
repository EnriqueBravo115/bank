package dev.enrique.bank.controller;

import java.math.BigDecimal;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;

import static dev.enrique.bank.commons.constants.PathConstants.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.dto.response.TransactionCommonResponse;
import dev.enrique.bank.dto.response.TransactionDetailedResponse;
import dev.enrique.bank.enums.TransactionType;
import dev.enrique.bank.service.TransactionAnalyticsService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(TRANSACTION_ANALYTICS)
@RequiredArgsConstructor
public class TransactionAnalyticsController {
    private final TransactionAnalyticsService transactionAnalyticsService;

    @GetMapping(GROUP_TRANSACTIONS_BY_TYPE)
    public ResponseEntity<Map<TransactionType, List<TransactionDetailedResponse>>> groupTransactionsByType(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(transactionAnalyticsService.groupTransactionsByType(accountId));
    }

    @GetMapping(SUM_TRANSACTIONS_BY_TYPE)
    public ResponseEntity<Map<TransactionType, BigDecimal>> sumTransactionsByType(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(transactionAnalyticsService.sumTransactionsByType(accountId));
    }

    @GetMapping(GET_TRANSACTION_YEAR_STATS)
    public ResponseEntity<IntSummaryStatistics> getTransactionYearStatistics(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(transactionAnalyticsService.getTransactionYearStatistics(accountId));
    }

    @GetMapping(PARTITION_TRANSACTIONS_BY_AMOUNT)
    public ResponseEntity<Map<Boolean, List<TransactionCommonResponse>>> partitionTransactionsByAmount(
            @PathVariable Long accountId,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(transactionAnalyticsService.partitionTransactionsByAmount(accountId, amount));
    }

    @GetMapping(GET_TRANSACTION_TYPE_SUMMARY)
    public ResponseEntity<Map<TransactionType, String>> getTransactionTypeSummary(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(transactionAnalyticsService.getTransactionTypeSummary(accountId));
    }

    @GetMapping(GET_TOTAL_TRANSACTION_AMOUNT)
    public ResponseEntity<BigDecimal> getTotalTransactionAmount(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(transactionAnalyticsService.calculateTotalTransactionAmount(accountId));
    }

    @GetMapping(GET_TOTAL_AMOUNT_BY_TYPE)
    public ResponseEntity<BigDecimal> getTotalAmountByType(
            @PathVariable Long accountId,
            @RequestParam TransactionType type) {
        return ResponseEntity.ok(transactionAnalyticsService.calculateTotalAmountByType(accountId, type));
    }

    @GetMapping(GET_AVERAGE_DAYS_BETWEEN)
    public ResponseEntity<Double> getAverageDaysBetweenTransactions(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(transactionAnalyticsService.getAverageDaysBetweenTransactions(accountId));
    }
}
