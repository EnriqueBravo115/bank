package dev.enrique.bank.controller;

import java.util.List;
import java.util.Set;

import static dev.enrique.bank.commons.constants.PathConstants.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.service.TransactionSupportService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class TransactionSupportController {
    private final TransactionSupportService transactionSupportService;

    @GetMapping(GET_UNIQUE_DESCRIPTIONS)
    public ResponseEntity<Set<String>> getUniqueTransactionDescriptions(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(transactionSupportService.getAllUniqueTransactionDescriptions(accountId));
    }

    @GetMapping(GET_ALL_DESCRIPTIONS)
    public ResponseEntity<String> getAllTransactionDescriptions(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(transactionSupportService.getAllTransactionDescriptions(accountId));
    }

    @PostMapping(GET_FORMATTED_AVERAGE_BALANCE)
    public ResponseEntity<String> getFormattedAverageBalance(
            @RequestBody List<Long> accountIds) {
        return ResponseEntity.ok(transactionSupportService.getFormattedAverageBalance(accountIds));
    }
}
