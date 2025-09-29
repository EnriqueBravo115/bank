package dev.enrique.bank.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static dev.enrique.bank.commons.constants.PathConstants.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.commons.dto.response.TransactionBasicResponse;
import dev.enrique.bank.commons.dto.response.TransactionCommonResponse;
import dev.enrique.bank.commons.dto.response.TransactionDetailedResponse;
import dev.enrique.bank.commons.dto.response.TransactionSummaryResponse;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.dao.projection.TransactionBasicProjection;
import dev.enrique.bank.service.TransactionAnalyticsService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(TRANSACTION_ANALYTICS)
@RequiredArgsConstructor
public class TransactionAnalyticsController {
    private final TransactionAnalyticsService transactionAnalyticsService;

    @GetMapping(GROUP_TRANSACTIONS_BY_TYPE)
    public ResponseEntity<Map<TransactionType, List<TransactionDetailedResponse>>> groupTransactionsByType(
            @PathVariable String accountNumber,
            @RequestParam TransactionStatus status) {
        return ResponseEntity.ok(transactionAnalyticsService.groupTransactionsByType(accountNumber, status));
    }

    @GetMapping(SUM_TRANSACTIONS_BY_TYPE)
    public ResponseEntity<Map<TransactionType, BigDecimal>> sumTransactionsByType(
            @PathVariable String accountNumber,
            @RequestParam TransactionStatus status) {
        return ResponseEntity.ok(transactionAnalyticsService.sumTransactionsByType(accountNumber, status));
    }

    @GetMapping(PARTITION_TRANSACTIONS_BY_AMOUNT)
    public ResponseEntity<Map<Boolean, List<TransactionBasicResponse>>> partitionTransactionsByAmount(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount,
            @RequestParam TransactionStatus status) {
        return ResponseEntity.ok(transactionAnalyticsService.partitionTransactionsByAmount(accountNumber, status, amount));
    }

    @GetMapping(GET_TRANSACTION_TYPE_SUMMARY)
    public ResponseEntity<Map<TransactionType, TransactionSummaryResponse>> getTransactionTypeSummary(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(transactionAnalyticsService.getTransactionTypeSummary(accountNumber));
    }

    @GetMapping(GET_TOTAL_TRANSACTION_AMOUNT)
    public ResponseEntity<BigDecimal> getTotalTransactionAmount(
            @PathVariable String accountNumber,
            @RequestParam TransactionStatus status) {
        return ResponseEntity.ok(transactionAnalyticsService.calculateTotalTransactionAmount(accountNumber, status));
    }

    @GetMapping(GET_TOTAL_AMOUNT_BY_TYPE)
    public ResponseEntity<BigDecimal> getTotalAmountByType(
            @PathVariable String accountNumber,
            @RequestParam TransactionType type) {
        return ResponseEntity.ok(transactionAnalyticsService.calculateTotalAmountByType(accountNumber, type));
    }

    @GetMapping(GET_AVERAGE_DAYS_BETWEEN)
    public ResponseEntity<Double> getAverageDaysBetweenTransactions(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(transactionAnalyticsService.getAverageDaysBetweenTransactions(accountNumber));
    }
}
