package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.GET_ALL_TRANSACTIONS;
import static dev.enrique.bank.commons.constants.PathConstants.GET_TRANSACTIONS_BY_ACCOUNT_AND_YEAR;
import static dev.enrique.bank.commons.constants.PathConstants.GET_TRANSACTIONS_FOR_ACCOUNTS;
import static dev.enrique.bank.commons.constants.PathConstants.GET_TRANSACTION_HISTORY;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.commons.dto.request.AccountYearRequest;
import dev.enrique.bank.commons.dto.response.HeaderResponse;
import dev.enrique.bank.commons.dto.response.TransactionCommonResponse;
import dev.enrique.bank.commons.dto.response.TransactionDetailedResponse;
import dev.enrique.bank.commons.enums.TransactionStatus;
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
            @PathVariable String accountNumber,
            @RequestParam TransactionStatus status) {
        return ResponseEntity.ok(transactionQueryService.getTransactionHistory(accountNumber, status));
    }

    @GetMapping(GET_ALL_TRANSACTIONS)
    public ResponseEntity<HeaderResponse<TransactionCommonResponse>> getAllTransactions(
            @PathVariable @NotNull @Positive String accountNumber,
            @RequestParam TransactionStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(transactionQueryService.getAllTransactions(accountNumber, status, pageable));
    }

    @GetMapping(GET_TRANSACTIONS_BY_ACCOUNT_AND_YEAR)
    public ResponseEntity<List<TransactionDetailedResponse>> getTransactionsByYear(
            @RequestBody AccountYearRequest request) {
        return ResponseEntity.ok(transactionQueryService
                .getTransactionByYear(request.getAccountNumber(), request.getYear()));
    }

    @PostMapping(GET_TRANSACTIONS_FOR_ACCOUNTS)
    public ResponseEntity<List<TransactionCommonResponse>> getTransactionsForAccounts(
            @RequestBody List<String> accountNumbers) {
        return ResponseEntity.ok(transactionQueryService.getAllTransactionsFromAccounts(accountNumbers));
    }
}
