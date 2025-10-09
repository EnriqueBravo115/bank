package dev.enrique.bank.controller;

import dev.enrique.bank.commons.dto.request.FilterStatusRequest;
import dev.enrique.bank.commons.dto.request.FilterStatusYearRequest;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
