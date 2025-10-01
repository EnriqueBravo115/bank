package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.TRANSACTION_ANALYTICS;

import java.math.BigDecimal;
import java.time.Month;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.commons.dto.request.FilterStatusAmountRequest;
import dev.enrique.bank.commons.dto.request.FilterStatusRequest;
import dev.enrique.bank.commons.dto.request.FilterStatusTypeRequest;
import dev.enrique.bank.commons.dto.response.TransactionBasicResponse;
import dev.enrique.bank.commons.dto.response.TransactionDetailedResponse;
import dev.enrique.bank.commons.dto.response.TransactionSummaryResponse;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.service.TransactionAnalyticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(TRANSACTION_ANALYTICS)
@RequiredArgsConstructor
public class TransactionAnalyticsController {
    private final TransactionAnalyticsService transactionAnalyticsService;

    @GetMapping("/group-by-type")
    public ResponseEntity<Map<TransactionType, List<TransactionDetailedResponse>>> groupTransactionsByType(
            @Valid FilterStatusRequest request) {
        return ResponseEntity.ok(transactionAnalyticsService
                .groupTransactionsByType(request.accountNumber(), request.status()));
    }

    @GetMapping("/sum-by-type")
    public ResponseEntity<Map<TransactionType, BigDecimal>> sumTransactionsByType(
            @Valid FilterStatusRequest request) {
        return ResponseEntity.ok(transactionAnalyticsService.sumTransactionsByType(request.accountNumber(),
                request.status()));
    }

    @GetMapping("/partition-by-amount")
    public ResponseEntity<Map<Boolean, List<TransactionBasicResponse>>> partitionTransactionsByAmount(
            @Valid FilterStatusAmountRequest request) {
        return ResponseEntity.ok(transactionAnalyticsService.partitionTransactionsByAmount(request.accountNumber(),
                request.status(), request.amount()));
    }

    @GetMapping("/type-summary")
    public ResponseEntity<Map<TransactionType, TransactionSummaryResponse>> getTransactionTypeSummary(
            @Valid FilterStatusRequest request) {
        return ResponseEntity
                .ok(transactionAnalyticsService.getTransactionTypeSummary(request.accountNumber(), request.status()));
    }

    @GetMapping("/total-amount")
    public ResponseEntity<BigDecimal> getTotalTransactionAmount(
            @Valid FilterStatusTypeRequest request) {
        return ResponseEntity.ok(transactionAnalyticsService
                .calculateTotalAmountByStatusAndType(request.accountNumber(), request.status(), request.type()));
    }

    @GetMapping("/average-days")
    public ResponseEntity<Double> getAverageDaysBetweenTransactions(
            @Valid FilterStatusRequest request) {
        return ResponseEntity.ok(transactionAnalyticsService.getAverageDaysBetweenTransactions(request.accountNumber(),
                request.status()));
    }

    @GetMapping("/max-by-type")
    public ResponseEntity<Map<TransactionType, List<TransactionBasicResponse>>> getMaxByTransactionType(
            @Valid FilterStatusRequest request) {
        return ResponseEntity
                .ok(transactionAnalyticsService.getMaxTransactionByType(request.accountNumber(), request.status()));
    }

    @GetMapping("/count-by-month")
    public ResponseEntity<Map<Month, Long>> countTransactionsByMonth(
            @Valid FilterStatusRequest accountStatusRequest) {
        return ResponseEntity.ok(transactionAnalyticsService
                .countTransactionsByMonth(accountStatusRequest.accountNumber(), accountStatusRequest.status()));
    }

    @GetMapping("/average-amount")
    public ResponseEntity<Map<TransactionType, BigDecimal>> getAverageAmountByType(
            @Valid FilterStatusRequest statusRequest) {
        return ResponseEntity.ok(transactionAnalyticsService
                .getAverageAmountByType(statusRequest.accountNumber(), statusRequest.status()));
    }
}
