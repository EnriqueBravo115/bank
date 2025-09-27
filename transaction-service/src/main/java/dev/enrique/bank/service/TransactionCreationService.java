package dev.enrique.bank.service;

import dev.enrique.bank.commons.dto.request.PurchaseRequest;
import dev.enrique.bank.commons.dto.request.ServiceRequest;
import dev.enrique.bank.commons.dto.request.TransferRequest;

public interface TransactionCreationService {
    void transfer(TransferRequest accountTransferRequest);
    void purchase(PurchaseRequest purchaseRequest);
    void service(ServiceRequest purchaseRequest);
    void cancelTransaction(Long transactionId);
}
