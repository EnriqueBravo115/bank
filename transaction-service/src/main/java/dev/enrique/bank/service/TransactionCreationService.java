package dev.enrique.bank.service;

import dev.enrique.bank.commons.dto.request.PurchaseRequest;
import dev.enrique.bank.commons.dto.request.ServiceRequest;
import dev.enrique.bank.commons.dto.request.TransferRequest;
import dev.enrique.bank.commons.dto.request.WithdrawalRequest;

public interface TransactionCreationService {
    void transfer(TransferRequest accountTransferRequest);
    void purchase(PurchaseRequest purchaseRequest);
    void servicePayment(ServiceRequest purchaseRequest);
    void withdrawal(WithdrawalRequest withdrawalRequest);
    void cancelTransaction(Long transactionId);
}
