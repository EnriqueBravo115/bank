package dev.enrique.bank.service;

import dev.enrique.bank.commons.dto.request.ClientPurchaseRequest;
import dev.enrique.bank.commons.dto.request.ClientServiceRequest;
import dev.enrique.bank.commons.dto.request.ClientTransferRequest;
import dev.enrique.bank.commons.dto.request.ClientWithdrawalRequest;
import dev.enrique.bank.commons.dto.response.MovementResultResponse;

public interface MovementService {
    MovementResultResponse processTransfer(ClientTransferRequest request);

    MovementResultResponse processPurchase(ClientPurchaseRequest request);

    MovementResultResponse processService(ClientServiceRequest request);

    MovementResultResponse processWithdrawal(ClientWithdrawalRequest request);
}
