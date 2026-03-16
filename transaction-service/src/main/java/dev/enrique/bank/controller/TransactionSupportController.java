package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.TRANSACTION_SUPPORT;

import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.service.TransactionSupportService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(TRANSACTION_SUPPORT)
@RequiredArgsConstructor
public class TransactionSupportController {
    private final TransactionSupportService transactionSupportService;

    @GetMapping("/unique-descriptions/{sourceIdentifier}")
    public ResponseEntity<Set<String>> getUniqueTransactionDescriptions(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(transactionSupportService.getAllUniqueTransactionDescriptions(accountNumber));
    }

    @GetMapping("/all-descriptions/{sourceIdentifier}")
    public ResponseEntity<String> getAllTransactionDescriptions(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(transactionSupportService.getAllTransactionDescriptions(accountNumber));
    }
}
