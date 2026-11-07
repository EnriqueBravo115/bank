package dev.enrique.bank.service;

import dev.enrique.bank.commons.dto.request.ClientPurchaseRequest;
import dev.enrique.bank.commons.dto.request.ClientServiceRequest;
import dev.enrique.bank.commons.dto.request.ClientTransferRequest;
import dev.enrique.bank.commons.dto.request.ClientWithdrawalRequest;
import dev.enrique.bank.commons.dto.response.MovementResultResponse;

public interface MovementService {
    MovementResultResponse processTransfer(String idempotencyKey, ClientTransferRequest request);

    MovementResultResponse processPurchase(String idempotencyKey, ClientPurchaseRequest request);

    MovementResultResponse processService(String idempotencyKey, ClientServiceRequest request);

    MovementResultResponse processWithdrawal(String idempotencyKey, ClientWithdrawalRequest request);

    MovementResultResponse reverse(String idempotencyKey);
}
