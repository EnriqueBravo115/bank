package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.*;

import java.math.BigDecimal;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.dto.response.HeaderResponse;
import dev.enrique.bank.dto.response.TransactionBasicResponse;
import dev.enrique.bank.dto.response.TransactionCommonResponse;
import dev.enrique.bank.dto.response.TransactionDetailedResponse;
import dev.enrique.bank.mapper.TransactionMapper;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(TRANSACTIONS)
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionMapper transactionMapper;

    @GetMapping(GET_ALL_TRANSACTIONS)
    public ResponseEntity<HeaderResponse<TransactionCommonResponse>> getAllTransactions(
            @PathVariable Long accountId,
            @PageableDefault(size = 20, sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(transactionMapper.getAllTransactions(accountId, pageable));
    }

    @GetMapping(GET_TRANSACTION_HISTORY)
    public ResponseEntity<List<TransactionDetailedResponse>> getTransactionHistory(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(transactionMapper.getTransactionHistory(accountId));
    }

    @GetMapping(GET_TRANSACTIONS_BY_YEAR)
    public ResponseEntity<List<TransactionCommonResponse>> getTransactionsByYear(
            @PathVariable Long accountId,
            @PathVariable Integer year) {
        return ResponseEntity.ok(transactionMapper.getTransactionsByYearAndAccount(accountId, year));
    }

    @GetMapping(GET_TRANSACTION_REVERSALS)
    public ResponseEntity<List<TransactionBasicResponse>> getTransactionReversals(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(transactionMapper.getAllTransactionsReversals(accountId));
    }

    @PostMapping(GET_TRANSACTIONS_FOR_ACCOUNTS)
    public ResponseEntity<List<TransactionCommonResponse>> getTransactionsForAccounts(
            @RequestBody List<Long> accountIds) {
        return ResponseEntity.ok(transactionMapper.getAllTransactionsFromAccounts(accountIds));
    }

    @GetMapping(GROUP_TRANSACTIONS_BY_TYPE)
    public ResponseEntity<Map<TransactionType, List<TransactionCommonResponse>>> groupTransactionsByType(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(transactionMapper.groupTransactionsByType(accountId));
    }

    @GetMapping(SUM_TRANSACTIONS_BY_TYPE)
    public ResponseEntity<Map<TransactionType, BigDecimal>> sumTransactionsByType(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(transactionMapper.sumTransactionsByType(accountId));
    }

    @GetMapping(GET_TRANSACTION_YEAR_STATS)
    public ResponseEntity<IntSummaryStatistics> getTransactionYearStatistics(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(transactionMapper.getTransactionYearStatistics(accountId));
    }

    @GetMapping(PARTITION_TRANSACTIONS_BY_AMOUNT)
    public ResponseEntity<Map<Boolean, List<TransactionBasicResponse>>> partitionTransactionsByAmount(
            @PathVariable Long accountId,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(transactionMapper.partitionTransactionsByAmount(accountId, amount));
    }

    @GetMapping(GET_TRANSACTION_TYPE_SUMMARY)
    public ResponseEntity<Map<TransactionType, String>> getTransactionTypeSummary(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(transactionMapper.getTransactionTypeSummary(accountId));
    }

    @GetMapping(GET_TOTAL_TRANSACTION_AMOUNT)
    public ResponseEntity<BigDecimal> getTotalTransactionAmount(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(transactionMapper.calculateTotalTransactionAmount(accountId));
    }

    @GetMapping(GET_TOTAL_AMOUNT_BY_TYPE)
    public ResponseEntity<BigDecimal> getTotalAmountByType(
            @PathVariable Long accountId,
            @RequestParam TransactionType type) {
        return ResponseEntity.ok(transactionMapper.calculateTotalAmountByType(accountId, type));
    }

    @GetMapping(CALCULATE_TRANSFER_FEE)
    public ResponseEntity<BigDecimal> calculateTransferFee(
            @RequestParam BigDecimal amount,
            @RequestParam Currency currency) {
        return ResponseEntity.ok(transactionMapper.calculateTransferFee(amount, currency));
    }

    @GetMapping(CHECK_SUFFICIENT_FUNDS)
    public ResponseEntity<Boolean> checkSufficientFunds(
            @PathVariable Long accountId,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(transactionMapper.hasSufficientFunds(accountId, amount));
    }

    @GetMapping(GET_TRANSFER_LIMIT)
    public ResponseEntity<BigDecimal> getTransferLimit(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(transactionMapper.getTransferLimit(accountId));
    }

    @GetMapping(GET_UNIQUE_DESCRIPTIONS)
    public ResponseEntity<Set<String>> getUniqueTransactionDescriptions(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(transactionMapper.getAllUniqueTransactionDescriptions(accountId));
    }

    @GetMapping(GET_ALL_DESCRIPTIONS)
    public ResponseEntity<String> getAllTransactionDescriptions(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(transactionMapper.getAllTransactionDescriptions(accountId));
    }

    @PostMapping(GET_FORMATTED_AVERAGE_BALANCE)
    public ResponseEntity<String> getFormattedAverageBalance(
            @RequestBody List<Long> accountIds) {
        return ResponseEntity.ok(transactionMapper.getFormattedAverageBalance(accountIds));
    }

    @GetMapping(FIND_MAX_TRANSACTION)
    public ResponseEntity<Optional<TransactionBasicResponse>> findMaxTransaction(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(transactionMapper.findMaxTransaction(accountId));
    }

    @GetMapping(GET_AVERAGE_DAYS_BETWEEN)
    public ResponseEntity<Double> getAverageDaysBetweenTransactions(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(transactionMapper.getAverageDaysBetweenTransactions(accountId));
    }
}
