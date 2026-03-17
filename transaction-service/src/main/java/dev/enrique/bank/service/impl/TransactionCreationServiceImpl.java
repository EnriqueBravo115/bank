package dev.enrique.bank.service.impl;

import org.springframework.stereotype.Service;

import dev.enrique.bank.commons.dto.request.PurchaseRequest;
import dev.enrique.bank.commons.dto.request.ServiceRequest;
import dev.enrique.bank.commons.dto.request.TransferRequest;
import dev.enrique.bank.commons.dto.request.WithdrawalRequest;
import dev.enrique.bank.commons.dto.response.TransactionResultResponse;
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
    public TransactionResultResponse transfer(TransferRequest transferRequest) {
        return transferTransactionProcessor.process(transferRequest);
    }

    @Override
    public TransactionResultResponse purchase(PurchaseRequest purchaseRequest) {
        return purchaseTransactionProcessor.process(purchaseRequest);
    }

    @Override
    public TransactionResultResponse servicePayment(ServiceRequest serviceRequest) {
        return serviceTransactionProcessor.process(serviceRequest);
    }

    @Override
    public TransactionResultResponse withdrawal(WithdrawalRequest withdrawalRequest) {
        return withdrawalTransactionProcessor.process(withdrawalRequest);
    }

    @Override
    public void cancelTransaction(Long transactionId) {
    }
}
