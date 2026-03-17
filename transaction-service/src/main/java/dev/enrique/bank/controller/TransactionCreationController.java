package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.TRANSACTION_CREATE;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.commons.dto.request.PurchaseRequest;
import dev.enrique.bank.commons.dto.request.ServiceRequest;
import dev.enrique.bank.commons.dto.request.TransferRequest;
import dev.enrique.bank.commons.dto.response.TransactionResultResponse;
import dev.enrique.bank.service.TransactionCreationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(TRANSACTION_CREATE)
@RequiredArgsConstructor
public class TransactionCreationController {
    private final TransactionCreationService transactionCreationService;

    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_BASIC')")
    @PostMapping("/transfer")
    public ResponseEntity<TransactionResultResponse> transfer(@Valid @RequestBody TransferRequest transferRequest) {
        return ResponseEntity.ok(transactionCreationService.transfer(transferRequest));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_BASIC')")
    @PostMapping("/purchase")
    public ResponseEntity<TransactionResultResponse> purchase(@Valid @RequestBody PurchaseRequest purchaseRequest) {
        return ResponseEntity.ok(transactionCreationService.purchase(purchaseRequest));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_BASIC')")
    @PostMapping("/service-payment")
    public ResponseEntity<TransactionResultResponse> servicePayment(@Valid @RequestBody ServiceRequest serviceRequest) {
        return ResponseEntity.ok(transactionCreationService.servicePayment(serviceRequest));
    }
}
