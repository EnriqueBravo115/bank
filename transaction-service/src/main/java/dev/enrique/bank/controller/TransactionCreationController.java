package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.PURCHASE;
import static dev.enrique.bank.commons.constants.PathConstants.SERVICE_PAYMENT;
import static dev.enrique.bank.commons.constants.PathConstants.TRANSACTION_CREATION;
import static dev.enrique.bank.commons.constants.PathConstants.TRANSFER;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.commons.dto.request.PurchaseRequest;
import dev.enrique.bank.commons.dto.request.ServiceRequest;
import dev.enrique.bank.commons.dto.request.TransferRequest;
import dev.enrique.bank.service.TransactionCreationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(TRANSACTION_CREATION)
@RequiredArgsConstructor
public class TransactionCreationController {
    private final TransactionCreationService transactionCreationService;

    @PostMapping(TRANSFER)
    public ResponseEntity<Void> transfer(@Valid @RequestBody TransferRequest transferRequest) {
        transactionCreationService.transfer(transferRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping(PURCHASE)
    public ResponseEntity<Void> purchase(@Valid @RequestBody PurchaseRequest purchaseRequest) {
        transactionCreationService.purchase(purchaseRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping(SERVICE_PAYMENT)
    public ResponseEntity<Void> service(@Valid @RequestBody ServiceRequest serviceRequest) {
        transactionCreationService.service(serviceRequest);
        return ResponseEntity.ok().build();
    }
}
