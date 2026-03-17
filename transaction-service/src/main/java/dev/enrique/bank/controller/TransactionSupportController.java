package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.TRANSACTION_SUPPORT;

import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_BASIC')")
    @GetMapping("/unique-descriptions/{accountNumber}")
    public ResponseEntity<Set<String>> getUniqueTransactionDescriptions(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(transactionSupportService.getAllUniqueTransactionDescriptions(accountNumber));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_BASIC')")
    @GetMapping("/all-descriptions/{accountNumber}")
    public ResponseEntity<String> getAllTransactionDescriptions(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(transactionSupportService.getAllTransactionDescriptions(accountNumber));
    }
}
