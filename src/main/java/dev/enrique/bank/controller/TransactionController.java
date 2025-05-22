package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.ALL;
import static dev.enrique.bank.commons.constants.PathConstants.API_V1_TRANSACTION;
import static dev.enrique.bank.commons.constants.PathConstants.HISTORY;
import static dev.enrique.bank.commons.constants.PathConstants.YEAR_AND_ACCOUNT_ID;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.dto.response.HeaderResponse;
import dev.enrique.bank.dto.response.TransactionCommonResponse;
import dev.enrique.bank.dto.response.TransactionDetailedResponse;
import dev.enrique.bank.mapper.TransactionMapper;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(API_V1_TRANSACTION)
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionMapper transactionMapper;

    @GetMapping(HISTORY)
    public ResponseEntity<List<TransactionDetailedResponse>> getTransactionHistory(
            @PathVariable("accountId") Long accountId) {
        return ResponseEntity.ok(transactionMapper.getTransactionHistoryAsResponse(accountId));
    }

    @GetMapping(YEAR_AND_ACCOUNT_ID)
    public ResponseEntity<List<TransactionCommonResponse>> getTransactionsByYear(
            @PathVariable("year") @Min(1900) @Max(2100) Integer year,
            @PathVariable("accountId") @Positive Long accountId) {
        return ResponseEntity.ok(transactionMapper.getTransactionsByYearAndAccount(accountId, year));
    }

    @GetMapping(ALL)
    public ResponseEntity<List<TransactionCommonResponse>> getAllTransactions(
            @PageableDefault(size = 15) Pageable pageable) {
        HeaderResponse<TransactionCommonResponse> response = transactionMapper.getAllTransactions(pageable);
        return ResponseEntity.ok().headers(response.getHeaders()).body(response.getItems());
    }
}
