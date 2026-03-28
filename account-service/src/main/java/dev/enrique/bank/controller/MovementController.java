package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.MOVEMENT;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.commons.dto.request.ClientPurchaseRequest;
import dev.enrique.bank.commons.dto.request.ClientServiceRequest;
import dev.enrique.bank.commons.dto.request.ClientTransferRequest;
import dev.enrique.bank.commons.dto.request.ClientWithdrawalRequest;
import dev.enrique.bank.commons.dto.response.MovementResultResponse;
import dev.enrique.bank.service.MovementService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(MOVEMENT)
@RequiredArgsConstructor
public class MovementController {
    private final MovementService movementService;

    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_BASIC')")
    @PostMapping("/transfer")
    public ResponseEntity<MovementResultResponse> processTransfer(
            @RequestBody ClientTransferRequest request) {
        return ResponseEntity.ok(movementService.processTransfer(request));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_BASIC')")
    @PostMapping("/purchase")
    public ResponseEntity<MovementResultResponse> processPurchase(
            @RequestBody ClientPurchaseRequest request) {
        return ResponseEntity.ok(movementService.processPurchase(request));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_BASIC')")
    @PostMapping("/service")
    public ResponseEntity<MovementResultResponse> processService(
            @RequestBody ClientServiceRequest request) {
        return ResponseEntity.ok(movementService.processService(request));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_BASIC')")
    @PostMapping("/withdrawal")
    public ResponseEntity<MovementResultResponse> processWithdrawal(
            @RequestBody ClientWithdrawalRequest request) {
        return ResponseEntity.ok(movementService.processWithdrawal(request));
    }
}
