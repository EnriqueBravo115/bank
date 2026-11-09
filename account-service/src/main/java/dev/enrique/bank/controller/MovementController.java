package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.Headers.IDEMPOTENCY_KEY;
import static dev.enrique.bank.commons.constants.PathConstants.MOVEMENT;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.commons.dto.request.ClientDepositRequest;
import dev.enrique.bank.commons.dto.request.ClientPurchaseRequest;
import dev.enrique.bank.commons.dto.request.ClientServiceRequest;
import dev.enrique.bank.commons.dto.request.ClientTransferRequest;
import dev.enrique.bank.commons.dto.request.ClientWithdrawalRequest;
import dev.enrique.bank.commons.dto.response.MovementResultResponse;
import dev.enrique.bank.service.MovementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

// Only transaction-service moves money (client credentials token with the SERVICE role): it has
// already checked that the source account belongs to the customer.
@RestController
@RequestMapping(MOVEMENT)
@RequiredArgsConstructor
@PreAuthorize("hasRole('SERVICE')")
public class MovementController {
    private final MovementService movementService;

    @PostMapping("/transfer")
    public ResponseEntity<MovementResultResponse> processTransfer(
            @RequestHeader(IDEMPOTENCY_KEY) @NotBlank @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody ClientTransferRequest request) {
        return ResponseEntity.ok(movementService.processTransfer(idempotencyKey, request));
    }

    @PostMapping("/purchase")
    public ResponseEntity<MovementResultResponse> processPurchase(
            @RequestHeader(IDEMPOTENCY_KEY) @NotBlank @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody ClientPurchaseRequest request) {
        return ResponseEntity.ok(movementService.processPurchase(idempotencyKey, request));
    }

    @PostMapping("/service")
    public ResponseEntity<MovementResultResponse> processService(
            @RequestHeader(IDEMPOTENCY_KEY) @NotBlank @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody ClientServiceRequest request) {
        return ResponseEntity.ok(movementService.processService(idempotencyKey, request));
    }

    @PostMapping("/withdrawal")
    public ResponseEntity<MovementResultResponse> processWithdrawal(
            @RequestHeader(IDEMPOTENCY_KEY) @NotBlank @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody ClientWithdrawalRequest request) {
        return ResponseEntity.ok(movementService.processWithdrawal(idempotencyKey, request));
    }

    @PostMapping("/deposit")
    public ResponseEntity<MovementResultResponse> processDeposit(
            @RequestHeader(IDEMPOTENCY_KEY) @NotBlank @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody ClientDepositRequest request) {
        return ResponseEntity.ok(movementService.processDeposit(idempotencyKey, request));
    }

    @PostMapping("/{idempotencyKey}/reverse")
    public ResponseEntity<MovementResultResponse> reverse(@PathVariable String idempotencyKey) {
        return ResponseEntity.ok(movementService.reverse(idempotencyKey));
    }
}
