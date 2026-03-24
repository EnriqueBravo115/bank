package dev.enrique.bank.controller;

import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/account/movement")
@RequiredArgsConstructor
public class MovementController {
    private final MovementService movementService;

    @PostMapping("/transfer")
    public ResponseEntity<MovementResultResponse> processTransfer(
            @RequestBody ClientTransferRequest request) {
        return ResponseEntity.ok(movementService.processTransfer(request));
    }

    @PostMapping("/purchase")
    public ResponseEntity<MovementResultResponse> processPurchase(
            @RequestBody ClientPurchaseRequest request) {
        return ResponseEntity.ok(movementService.processPurchase(request));
    }

    @PostMapping("/service")
    public ResponseEntity<MovementResultResponse> processService(
            @RequestBody ClientServiceRequest request) {
        return ResponseEntity.ok(movementService.processService(request));
    }

    @PostMapping("/withdrawal")
    public ResponseEntity<MovementResultResponse> processWithdrawal(
            @RequestBody ClientWithdrawalRequest request) {
        return ResponseEntity.ok(movementService.processWithdrawal(request));
    }
}
