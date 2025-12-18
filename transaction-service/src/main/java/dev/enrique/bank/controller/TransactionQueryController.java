package dev.enrique.bank.controller;

import dev.enrique.bank.commons.dto.request.*;
import dev.enrique.bank.commons.dto.response.HeaderResponse;
import dev.enrique.bank.commons.dto.response.TransactionCommonResponse;
import dev.enrique.bank.commons.dto.response.TransactionDetailedResponse;
import dev.enrique.bank.commons.util.BasicMapper;
import dev.enrique.bank.service.TransactionQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import static dev.enrique.bank.commons.constants.PathConstants.TRANSACTION_QUERY;

@RestController
@RequestMapping(TRANSACTION_QUERY)
@RequiredArgsConstructor
public class TransactionQueryController {
    private final TransactionQueryService transactionQueryService;
    private final BasicMapper basicMapper;

    @GetMapping("/history")
    public ResponseEntity<List<TransactionDetailedResponse>> getTransactionHistory(
            @Valid FilterStatusRequest request) {
        return ResponseEntity.ok(basicMapper.convertToResponseList(transactionQueryService.getTransactionHistory(
                request.sourceIdentifier(),
                request.status()), TransactionDetailedResponse.class));
    }

    @GetMapping("/all")
    public ResponseEntity<HeaderResponse<TransactionCommonResponse>> getAllTransactions(
            @Valid FilterStatusRequest request,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(basicMapper.getHeaderResponse(transactionQueryService.getAllTransactions(
                request.sourceIdentifier(),
                request.status(),
                pageable), TransactionCommonResponse.class));
    }

    @GetMapping("/status-year")
    public ResponseEntity<List<TransactionDetailedResponse>> getTransactionsByYear(
            @Valid FilterStatusYearRequest request) {
        return ResponseEntity.ok(basicMapper.convertToResponseList(transactionQueryService.getTransactionsByYear(
                request.sourceIdentifier(),
                request.status(),
                request.year()), TransactionDetailedResponse.class));
    }

    // This controller must be executed with "ADMIN" authorities
    @GetMapping("/by-sources")
    public ResponseEntity<List<TransactionCommonResponse>> getTransactionsBySourceIdentifiers(
            @RequestBody List<String> sourceIdentifiers) {
        return ResponseEntity.ok(basicMapper.convertToResponseList(transactionQueryService.getAllTransactionsBySourceIdentifiers(
                sourceIdentifiers), TransactionCommonResponse.class));
    }

    @GetMapping("/max")
    public ResponseEntity<Optional<TransactionCommonResponse>> getMaxTransaction(
            @Valid FilterStatusRequest request) {
        return ResponseEntity.ok(basicMapper.convertOptionalResponse(transactionQueryService.getMaxTransaction(
                request.sourceIdentifier(),
                request.status()), TransactionCommonResponse.class));
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<TransactionCommonResponse>> getTransactionInDateRange(
            @Valid FilterStatusBetweenDateRequest request) {
        return ResponseEntity.ok(basicMapper.convertToResponseList(transactionQueryService.getTransactionsInDateRange(
                request.sourceIdentifier(),
                request.status(),
                request.startDate(),
                request.endDate()), TransactionCommonResponse.class));
    }

    @GetMapping("/amount-range")
    public ResponseEntity<List<TransactionCommonResponse>> getAllByAmountRangeAndStatus(
            @Valid FilterStatusMinMaxAmountRequest request) {
        return ResponseEntity.ok(basicMapper.convertToResponseList(transactionQueryService.getAllTransactionByAmountRangeAndStatus(
                request.sourceIdentifier(),
                request.status(),
                request.minAmount(),
                request.maxAmount()), TransactionCommonResponse.class));
    }

    @GetMapping("/{transactionCode}")
    public ResponseEntity<Optional<TransactionDetailedResponse>> getTransactionByTransactionCode(
            @PathVariable String transactionCode) {
        return ResponseEntity.ok(basicMapper.convertOptionalResponse(transactionQueryService.getTransactionByCode(
                transactionCode), TransactionDetailedResponse.class));
    }

    @GetMapping("/identifier-type")
    public ResponseEntity<List<TransactionCommonResponse>> getAllByIdentifierTypeAndStatus(
            @Valid FilterStatusIdentifierTypeRequest request) {
        return ResponseEntity.ok(basicMapper.convertToResponseList(transactionQueryService.getAllByIdentifierTypeAndStatus(
                request.sourceIdentifier(),
                request.identifierType(),
                request.status()), TransactionCommonResponse.class));
    }

    @GetMapping("/keyword")
    public ResponseEntity<List<TransactionDetailedResponse>> getTransactionByKeyword(
            @Valid FilterStatusKeywordRequest request) {
        return ResponseEntity.ok(basicMapper.convertToResponseList(transactionQueryService.getTransactionByKeyword(
                request.sourceIdentifier(),
                request.status(),
                request.keyword()), TransactionDetailedResponse.class));
    }
}
