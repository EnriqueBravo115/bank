package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.TRANSACTION_CREATE;

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
@RequestMapping(TRANSACTION_CREATE)
@RequiredArgsConstructor
public class TransactionCreationController {
    private final TransactionCreationService transactionCreationService;

    @PostMapping("/transfer")
    public ResponseEntity<Void> transfer(@Valid @RequestBody TransferRequest transferRequest) {
        transactionCreationService.transfer(transferRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/purchase")
    public ResponseEntity<Void> purchase(@Valid @RequestBody PurchaseRequest purchaseRequest) {
        transactionCreationService.purchase(purchaseRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/service-payment")
    public ResponseEntity<Void> servicePayment(@Valid @RequestBody ServiceRequest serviceRequest) {
        transactionCreationService.servicePayment(serviceRequest);
        return ResponseEntity.ok().build();
    }
}
