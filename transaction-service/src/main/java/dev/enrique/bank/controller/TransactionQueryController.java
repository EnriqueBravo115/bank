package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.TRANSACTION_QUERY;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.commons.dto.response.HeaderResponse;
import dev.enrique.bank.commons.dto.response.TransactionCommonResponse;
import dev.enrique.bank.commons.dto.response.TransactionDetailedResponse;
import dev.enrique.bank.commons.enums.IdentifierType;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.util.BasicMapper;
import dev.enrique.bank.service.TransactionQueryService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(TRANSACTION_QUERY)
@RequiredArgsConstructor
public class TransactionQueryController {
    private final TransactionQueryService transactionQueryService;
    private final BasicMapper basicMapper;

    @GetMapping("/history")
    public ResponseEntity<List<TransactionDetailedResponse>> getTransactionHistory(
            @RequestParam String sourceIdentifier,
            @RequestParam TransactionStatus status) {
        return ResponseEntity.ok(basicMapper.convertToResponseList(
                transactionQueryService.getTransactionHistory(sourceIdentifier, status),
                TransactionDetailedResponse.class));
    }

    @GetMapping("/all")
    public ResponseEntity<HeaderResponse<TransactionCommonResponse>> getAllTransactions(
            @RequestParam String sourceIdentifier,
            @RequestParam TransactionStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(basicMapper.getHeaderResponse(
                transactionQueryService.getAllTransactions(sourceIdentifier, status, pageable),
                TransactionCommonResponse.class));
    }

    @GetMapping("/status-year")
    public ResponseEntity<List<TransactionDetailedResponse>> getTransactionsByYear(
            @RequestParam String sourceIdentifier,
            @RequestParam TransactionStatus status,
            @RequestParam Integer year) {
        return ResponseEntity.ok(basicMapper.convertToResponseList(
                transactionQueryService.getTransactionsByYear(sourceIdentifier, status, year),
                TransactionDetailedResponse.class));
    }

    // This controller must be executed with "ADMIN" authorities
    @GetMapping("/by-sources")
    public ResponseEntity<List<TransactionCommonResponse>> getTransactionsBySourceIdentifiers(
            @RequestBody List<String> sourceIdentifiers) {
        return ResponseEntity.ok(basicMapper.convertToResponseList(
                transactionQueryService.getAllTransactionsBySourceIdentifiers(sourceIdentifiers),
                TransactionCommonResponse.class));
    }

    // FIX: return null on "transactionCode" & "description"
    @GetMapping("/max")
    public ResponseEntity<Optional<TransactionCommonResponse>> getMaxTransaction(
            @RequestParam String sourceIdentifier,
            @RequestParam TransactionStatus status) {
        return ResponseEntity.ok(basicMapper.convertOptionalResponse(
                transactionQueryService.getMaxTransaction(sourceIdentifier, status),
                TransactionCommonResponse.class));
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<TransactionCommonResponse>> getTransactionInDateRange(
            @RequestParam String sourceIdentifier,
            @RequestParam TransactionStatus status,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(basicMapper.convertToResponseList(
                transactionQueryService.getTransactionsInDateRange(sourceIdentifier, status, startDate, endDate),
                TransactionCommonResponse.class));
    }

    @GetMapping("/amount-range")
    public ResponseEntity<List<TransactionCommonResponse>> getAllByAmountRangeAndStatus(
            @RequestParam String sourceIdentifier,
            @RequestParam TransactionStatus status,
            @RequestParam BigDecimal minAmount,
            @RequestParam BigDecimal maxAmount) {
        return ResponseEntity.ok(basicMapper.convertToResponseList(
                transactionQueryService.getAllTransactionByAmountRangeAndStatus(
                        sourceIdentifier, status, minAmount, maxAmount),
                TransactionCommonResponse.class));
    }

    @GetMapping("/{transactionCode}")
    public ResponseEntity<Optional<TransactionDetailedResponse>> getTransactionByTransactionCode(
            @PathVariable String transactionCode) {
        return ResponseEntity.ok(basicMapper.convertOptionalResponse(
                transactionQueryService.getTransactionByCode(transactionCode),
                TransactionDetailedResponse.class));
    }

    @GetMapping("/identifier-type")
    public ResponseEntity<List<TransactionCommonResponse>> getAllByIdentifierTypeAndStatus(
            @RequestParam String sourceIdentifier,
            @RequestParam TransactionStatus status,
            @RequestParam IdentifierType identifierType) {
        return ResponseEntity.ok(basicMapper.convertToResponseList(
                transactionQueryService.getAllByIdentifierTypeAndStatus(sourceIdentifier, identifierType, status),
                TransactionCommonResponse.class));
    }

    @GetMapping("/keyword")
    public ResponseEntity<List<TransactionDetailedResponse>> getTransactionByKeyword(
            @RequestParam String sourceIdentifier,
            @RequestParam TransactionStatus status,
            @RequestParam String keyword) {
        return ResponseEntity.ok(basicMapper.convertToResponseList(
                transactionQueryService.getTransactionByKeyword(sourceIdentifier, status, keyword),
                TransactionDetailedResponse.class));
    }
}
