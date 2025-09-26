package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.GET_ALL_TRANSACTIONS;
import static dev.enrique.bank.commons.constants.PathConstants.GET_TRANSACTIONS_FOR_ACCOUNTS;
import static dev.enrique.bank.commons.constants.PathConstants.GET_TRANSACTION_HISTORY;
import static dev.enrique.bank.commons.constants.PathConstants.GET_TRANSACTION_REVERSALS;
import static dev.enrique.bank.commons.constants.PathConstants.GET_TRANSACTIONS_BY_ACCOUNT_AND_YEAR;
import static dev.enrique.bank.commons.constants.PathConstants.TRANSACTION_QUERY;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.commons.dto.request.AccountYearRequest;
import dev.enrique.bank.commons.dto.response.HeaderResponse;
import dev.enrique.bank.commons.dto.response.TransactionCommonResponse;
import dev.enrique.bank.commons.dto.response.TransactionDetailedResponse;
import dev.enrique.bank.service.TransactionQueryService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(TRANSACTION_QUERY)
@RequiredArgsConstructor
public class TransactionQueryController {
    private final TransactionQueryService transactionQueryService;

    @GetMapping(GET_TRANSACTION_HISTORY)
    public ResponseEntity<List<TransactionDetailedResponse>> getTransactionHistory(
            @PathVariable @NotNull @Positive Long accountId) {
        return ResponseEntity.ok(transactionQueryService.getTransactionHistory(accountId));
    }

    @GetMapping(GET_ALL_TRANSACTIONS)
    public ResponseEntity<HeaderResponse<TransactionCommonResponse>> getAllTransactions(
            @PathVariable @NotNull @Positive Long accountId,
            @PageableDefault(size = 20, sort = "transactionDate") Pageable pageable) {
        return ResponseEntity.ok(transactionQueryService.getAllTransactions(accountId, pageable));
    }

    @GetMapping(GET_TRANSACTIONS_BY_ACCOUNT_AND_YEAR)
    public ResponseEntity<List<TransactionDetailedResponse>> getTransactionsByYear(
            @RequestBody AccountYearRequest request) {
        return ResponseEntity.ok(transactionQueryService
                .getTransactionByYearAndAccount(request.getAccountId(), request.getYear()));
    }

    @GetMapping(GET_TRANSACTION_REVERSALS)
    public ResponseEntity<List<TransactionCommonResponse>> getTransactionReversals(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(transactionQueryService.getAllTransactionsReversals(accountId));
    }

    @PostMapping(GET_TRANSACTIONS_FOR_ACCOUNTS)
    public ResponseEntity<List<TransactionCommonResponse>> getTransactionsForAccounts(
            @RequestBody List<Long> accountIds) {
        return ResponseEntity.ok(transactionQueryService.getAllTransactionsFromAccounts(accountIds));
    }
}
