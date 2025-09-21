package dev.enrique.bank.service;

import java.math.BigDecimal;

import dev.enrique.bank.dto.request.PurchaseRequest;
import dev.enrique.bank.dto.request.ServiceRequest;
import dev.enrique.bank.dto.request.TransferRequest;

public interface TransactionCreationService {
    void transferBetweenAccounts(TransferRequest accountTransferRequest);
    void purchaseWithCard(PurchaseRequest purchaseRequest);
    void servicePayment(ServiceRequest purchaseRequest);
    void cancelTransaction(Long transactionId);
}
