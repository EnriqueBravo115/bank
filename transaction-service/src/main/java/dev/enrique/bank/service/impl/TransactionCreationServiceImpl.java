package dev.enrique.bank.service.impl;

import org.springframework.stereotype.Service;

import dev.enrique.bank.commons.dto.request.PurchaseRequest;
import dev.enrique.bank.commons.dto.request.ServiceRequest;
import dev.enrique.bank.commons.dto.request.TransferRequest;
import dev.enrique.bank.commons.dto.request.WithdrawalRequest;
import dev.enrique.bank.commons.util.PurchaseTransactionProcessor;
import dev.enrique.bank.commons.util.ServiceTransactionProcessor;
import dev.enrique.bank.commons.util.TransferTransactionProcessor;
import dev.enrique.bank.commons.util.WithdrawalTransactionProcessor;
import dev.enrique.bank.service.TransactionCreationService;
import lombok.RequiredArgsConstructor;

// TODO: create reverseTransfer
@Service
@RequiredArgsConstructor
public class TransactionCreationServiceImpl implements TransactionCreationService {
    private final TransferTransactionProcessor transferTransactionProcessor;
    private final PurchaseTransactionProcessor purchaseTransactionProcessor;
    private final ServiceTransactionProcessor serviceTransactionProcessor;
    private final WithdrawalTransactionProcessor withdrawalTransactionProcessor;

    @Override
    public void transfer(TransferRequest transferRequest) {
        transferTransactionProcessor.process(transferRequest);
    }

    @Override
    public void purchase(PurchaseRequest purchaseRequest) {
        purchaseTransactionProcessor.process(purchaseRequest);
    }

    @Override
    public void service(ServiceRequest serviceRequest) {
        serviceTransactionProcessor.process(serviceRequest);
    }

    @Override
    public void withdrawal(WithdrawalRequest withdrawalRequest) {
        withdrawalTransactionProcessor.process(withdrawalRequest);
    }

    @Override
    public void cancelTransaction(Long transactionId) {
    }
}
