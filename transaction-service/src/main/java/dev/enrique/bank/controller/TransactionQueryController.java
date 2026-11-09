package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.TRANSACTION_QUERY;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.commons.constants.Paging;
import dev.enrique.bank.commons.dto.response.TransactionResponse;
import dev.enrique.bank.commons.enums.IdentifierType;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.service.AccountResolver;
import dev.enrique.bank.service.TransactionQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * Queries by source account. {@code sourceIdentifier} is an account number unless
 * {@code sourceIdentifierType} says otherwise (CLABE): either way it returns every transaction of the
 * account, whichever identifier was used to create them.
 */
@RestController
@RequestMapping(TRANSACTION_QUERY)
@RequiredArgsConstructor
public class TransactionQueryController {
    private final TransactionQueryService transactionQueryService;
    private final AccountResolver accountResolver;

    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_BASIC')")
    @GetMapping("/history")
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory(
            @RequestParam String sourceIdentifier,
            @RequestParam(defaultValue = "ACCOUNT") IdentifierType sourceIdentifierType,
            @RequestParam TransactionStatus status) {
        return ResponseEntity.ok(transactionQueryService.getTransactionHistory(
                accountResolver.accountNumberOf(sourceIdentifier, sourceIdentifierType), status));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_BASIC')")
    @GetMapping("/all")
    public ResponseEntity<PagedModel<TransactionResponse>> getAllTransactions(
            @RequestParam String sourceIdentifier,
            @RequestParam(defaultValue = "ACCOUNT") IdentifierType sourceIdentifierType,
            @RequestParam TransactionStatus status,
            @RequestParam(defaultValue = Paging.DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = Paging.DEFAULT_SIZE) @Min(1) @Max(Paging.MAX_SIZE) int size) {
        return ResponseEntity.ok(new PagedModel<>(transactionQueryService.getAllTransactions(
                accountResolver.accountNumberOf(sourceIdentifier, sourceIdentifierType), status,
                PageRequest.of(page, size))));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_BASIC')")
    @GetMapping("/status-year")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByYear(
            @RequestParam String sourceIdentifier,
            @RequestParam(defaultValue = "ACCOUNT") IdentifierType sourceIdentifierType,
            @RequestParam TransactionStatus status,
            @RequestParam Integer year) {
        return ResponseEntity.ok(transactionQueryService.getTransactionsByYear(
                accountResolver.accountNumberOf(sourceIdentifier, sourceIdentifierType), status, year));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/by-accounts")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByAccountNumbers(
            @RequestParam List<String> accountNumbers) {
        return ResponseEntity.ok(transactionQueryService.getAllTransactionsByAccountNumbers(accountNumbers));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_BASIC')")
    @GetMapping("/max")
    public ResponseEntity<TransactionResponse> getMaxTransaction(
            @RequestParam String sourceIdentifier,
            @RequestParam(defaultValue = "ACCOUNT") IdentifierType sourceIdentifierType,
            @RequestParam TransactionStatus status) {
        return ResponseEntity.ok(transactionQueryService.getMaxTransaction(
                accountResolver.accountNumberOf(sourceIdentifier, sourceIdentifierType), status));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_BASIC')")
    @GetMapping("/date-range")
    public ResponseEntity<List<TransactionResponse>> getTransactionInDateRange(
            @RequestParam String sourceIdentifier,
            @RequestParam(defaultValue = "ACCOUNT") IdentifierType sourceIdentifierType,
            @RequestParam TransactionStatus status,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(transactionQueryService.getTransactionsInDateRange(
                accountResolver.accountNumberOf(sourceIdentifier, sourceIdentifierType), status, startDate, endDate));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_BASIC')")
    @GetMapping("/amount-range")
    public ResponseEntity<List<TransactionResponse>> getAllByAmountRangeAndStatus(
            @RequestParam String sourceIdentifier,
            @RequestParam(defaultValue = "ACCOUNT") IdentifierType sourceIdentifierType,
            @RequestParam TransactionStatus status,
            @RequestParam BigDecimal minAmount,
            @RequestParam BigDecimal maxAmount) {
        return ResponseEntity.ok(transactionQueryService.getAllTransactionByAmountRangeAndStatus(
                accountResolver.accountNumberOf(sourceIdentifier, sourceIdentifierType), status, minAmount, maxAmount));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_BASIC')")
    @GetMapping("/{transactionCode}")
    public ResponseEntity<TransactionResponse> getTransactionByTransactionCode(@PathVariable String transactionCode) {
        return ResponseEntity.ok(transactionQueryService.getTransactionByCode(transactionCode));
    }

    // identifierType: the identifier the transactions were created with (account number or CLABE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_BASIC')")
    @GetMapping("/identifier-type")
    public ResponseEntity<List<TransactionResponse>> getAllByIdentifierTypeAndStatus(
            @RequestParam String sourceIdentifier,
            @RequestParam(defaultValue = "ACCOUNT") IdentifierType sourceIdentifierType,
            @RequestParam TransactionStatus status,
            @RequestParam IdentifierType identifierType) {
        return ResponseEntity.ok(transactionQueryService.getAllByIdentifierTypeAndStatus(
                accountResolver.accountNumberOf(sourceIdentifier, sourceIdentifierType), identifierType, status));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_BASIC')")
    @GetMapping("/keyword")
    public ResponseEntity<List<TransactionResponse>> getTransactionByKeyword(
            @RequestParam String sourceIdentifier,
            @RequestParam(defaultValue = "ACCOUNT") IdentifierType sourceIdentifierType,
            @RequestParam TransactionStatus status,
            @RequestParam String keyword) {
        return ResponseEntity.ok(transactionQueryService.getTransactionByKeyword(
                accountResolver.accountNumberOf(sourceIdentifier, sourceIdentifierType), status, keyword));
    }
}
