package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.TRANSACTION;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.commons.dto.response.TransactionResultResponse;
import dev.enrique.bank.service.TransactionCreationService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(TRANSACTION)
@RequiredArgsConstructor
public class TransactionLifecycleController {
    private final TransactionCreationService transactionCreationService;

    // The owner gives up a transaction whose result is still unknown (PENDING).
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_BASIC')")
    @PostMapping("/{transactionCode}/cancel")
    public ResponseEntity<TransactionResultResponse> cancel(@PathVariable String transactionCode) {
        return ResponseEntity.ok(transactionCreationService.cancel(transactionCode));
    }

    // Returning the money of a COMPLETED transaction is a back-office decision.
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{transactionCode}/reverse")
    public ResponseEntity<TransactionResultResponse> reverse(@PathVariable String transactionCode) {
        return ResponseEntity.ok(transactionCreationService.reverse(transactionCode));
    }
}
