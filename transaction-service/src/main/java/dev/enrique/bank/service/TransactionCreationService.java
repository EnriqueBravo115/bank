package dev.enrique.bank.service;

import dev.enrique.bank.commons.dto.request.PurchaseRequest;
import dev.enrique.bank.commons.dto.request.ServiceRequest;
import dev.enrique.bank.commons.dto.request.TransferRequest;
import dev.enrique.bank.commons.dto.request.WithdrawalRequest;
import dev.enrique.bank.commons.dto.response.TransactionResultResponse;

public interface TransactionCreationService {
    TransactionResultResponse transfer(TransferRequest accountTransferRequest);
    TransactionResultResponse purchase(PurchaseRequest purchaseRequest);
    TransactionResultResponse servicePayment(ServiceRequest purchaseRequest);
    TransactionResultResponse withdrawal(WithdrawalRequest withdrawalRequest);
    void cancelTransaction(Long transactionId);
}
