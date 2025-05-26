package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.ALL;
import static dev.enrique.bank.commons.constants.PathConstants.API_V1_TRANSACTION;
import static dev.enrique.bank.commons.constants.PathConstants.HISTORY_ID;
import static dev.enrique.bank.commons.constants.PathConstants.YEAR_AND_ACCOUNT_ID;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.dto.response.HeaderResponse;
import dev.enrique.bank.dto.response.TransactionBasicResponse;
import dev.enrique.bank.dto.response.TransactionCommonResponse;
import dev.enrique.bank.dto.response.TransactionDetailedResponse;
import dev.enrique.bank.mapper.TransactionMapper;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(API_V1_TRANSACTION)
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionMapper transactionMapper;

    @GetMapping(HISTORY_ID)
    public ResponseEntity<List<TransactionDetailedResponse>> getTransactionHistory(
            @PathVariable("accountId") Long accountId) {
        return ResponseEntity.ok(transactionMapper.getTransactionHistoryAsResponse(accountId));
    }

    @GetMapping(YEAR_AND_ACCOUNT_ID)
    public ResponseEntity<List<TransactionCommonResponse>> getTransactionsByYear(
            @PathVariable("year") Integer year,
            @PathVariable("accountId") @Positive Long accountId) {
        return ResponseEntity.ok(transactionMapper.getTransactionsByYearAndAccount(accountId, year));
    }

    @GetMapping(ALL)
    public ResponseEntity<List<TransactionCommonResponse>> getAllTransactions(
            @PathVariable Long accountId,
            @PageableDefault(size = 15) Pageable pageable) {
        HeaderResponse<TransactionCommonResponse> response = transactionMapper.getAllTransactions(accountId, pageable);
        return ResponseEntity.ok().headers(response.getHeaders()).body(response.getItems());
    }

    @GetMapping()
    public ResponseEntity<List<TransactionBasicResponse>> getAllTransactionsReversals(
            @PathVariable("accountId") Long accountId) {
        return ResponseEntity.ok(transactionMapper.getAllTransactionsReversals(accountId));
    }

    @GetMapping()
    public ResponseEntity<BigDecimal> calculateTotalTransactionAmount(
            @PathVariable("accountId") Long accountId) {
        return ResponseEntity.ok(transactionMapper.calculateTotalTransactionAmount(accountId));
    }

    @GetMapping()
    public ResponseEntity<BigDecimal> calculateTotalAmountByType(
            @PathVariable("accountId") Long accountId) {
        return ResponseEntity.ok(transactionMapper.calculateTotalAmountByType(accountId, type));
    }

    @GetMapping()
    public ResponseEntity<Boolean> hasSufficientFunds(
            @PathVariable("accountId") Long accountId,
            @PathVariable("amount") BigDecimal amount) {
        return ResponseEntity.ok(transactionMapper.hasSufficientFunds(accountId, amount));
    }
}
