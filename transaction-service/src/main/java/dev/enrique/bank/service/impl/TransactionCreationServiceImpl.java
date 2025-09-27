package dev.enrique.bank.service.impl;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.enrique.bank.client.AccountClient;
import dev.enrique.bank.commons.dto.request.AccountPurchaseRequest;
import dev.enrique.bank.commons.dto.request.AccountServiceRequest;
import dev.enrique.bank.commons.dto.request.AccountTransferRequest;
import dev.enrique.bank.commons.dto.request.PurchaseRequest;
import dev.enrique.bank.commons.dto.request.ServiceRequest;
import dev.enrique.bank.commons.dto.request.TransferRequest;
import dev.enrique.bank.commons.dto.response.MovementResultResponse;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.commons.util.BasicMapper;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.model.PurchaseTransaction;
import dev.enrique.bank.model.ServiceTransaction;
import dev.enrique.bank.model.Transaction;
import dev.enrique.bank.model.TransferTransaction;
import dev.enrique.bank.service.TransactionCreationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// TODO: create reverseTransfer
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TransactionCreationServiceImpl implements TransactionCreationService {
    private final AccountClient accountClient;
    private final TransactionRepository transactionRepository;
    private final BasicMapper basicMapper;

    @Override
    public void transfer(TransferRequest transferRequest) {
        String transactionCode = UUID.randomUUID().toString();

        try {
            log.debug("Start transfer transaction: {}", transactionCode);

            AccountTransferRequest request = basicMapper.mapTo(transferRequest, AccountTransferRequest.class);
            MovementResultResponse response = accountClient.processTransfer(request);

            TransferTransaction transferTransaction = TransferTransaction.builder()
                    .sourceAccountNumber(transferRequest.sourceAccountNumber())
                    .targetAccountNumber(transferRequest.targetAccountNumber())
                    .build();

            Transaction transaction = Transaction.builder()
                    .transactionCode(transactionCode)
                    .amount(transferRequest.amount())
                    .description(transferRequest.description())
                    .reason(response.reason())
                    .transactionDate(LocalDateTime.now())
                    .currency(transferRequest.currency())
                    .transactionType(TransactionType.TRANSFER)
                    .transactionStatus(response.transactionStatus())
                    .build();

            transaction.setTransferTransaction(transferTransaction);
            transferTransaction.setTransaction(transaction);

            transactionRepository.save(transaction);
            log.debug("End transfer transaction: {}", transactionCode);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error processing transfer transaction", e);
        }
    }

    @Override
    public void purchase(PurchaseRequest purchaseRequest) {
        String transactionCode = UUID.randomUUID().toString();

        try {
            log.debug("Start purchase transaction: {}", transactionCode);
            AccountPurchaseRequest request = basicMapper.mapTo(purchaseRequest, AccountPurchaseRequest.class);
            MovementResultResponse response = accountClient.processPurchase(request);

            PurchaseTransaction purchaseTransaction = PurchaseTransaction.builder()
                    .sourceCardNumber(purchaseRequest.sourceCardNumber())
                    .cvv(purchaseRequest.cvv())
                    .merchantCode(purchaseRequest.merchantCode())
                    .merchantCategory(purchaseRequest.merchantCategory())
                    .posId(purchaseRequest.posId())
                    .build();

            Transaction transaction = Transaction.builder()
                    .transactionCode(transactionCode)
                    .amount(purchaseRequest.amount())
                    .description(purchaseRequest.description())
                    .reason(response.reason())
                    .transactionDate(LocalDateTime.now())
                    .currency(purchaseRequest.currency())
                    .transactionType(TransactionType.TRANSFER)
                    .transactionStatus(response.transactionStatus())
                    .build();

            transaction.setPurchaseTransaction(purchaseTransaction);
            purchaseTransaction.setTransaction(transaction);

            transactionRepository.save(transaction);
            log.debug("End purchase transaction: {}", transactionCode);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error processing purchase transaction", e);
        }
    }

    @Override
    public void service(ServiceRequest serviceRequest) {
        String transactionCode = UUID.randomUUID().toString();

        try {
            log.debug("Start service transaction: {}", transactionCode);
            AccountServiceRequest request = basicMapper.mapTo(serviceRequest, AccountServiceRequest.class);
            MovementResultResponse response = accountClient.processService(request);

            ServiceTransaction serviceTransaction = ServiceTransaction.builder()
                    .sourceAccountNumber(serviceRequest.sourceAccountNumber())
                    .paymentReference(serviceRequest.paymentReference())
                    .serviceType(serviceRequest.serviceType())
                    .build();

            Transaction transaction = Transaction.builder()
                    .transactionCode(transactionCode)
                    .amount(serviceRequest.amount())
                    .description(serviceRequest.description())
                    .reason(response.reason())
                    .transactionDate(LocalDateTime.now())
                    .currency(serviceRequest.currency())
                    .transactionType(TransactionType.TRANSFER)
                    .transactionStatus(response.transactionStatus())
                    .build();

            transaction.setServiceTransaction(serviceTransaction);
            serviceTransaction.setTransaction(transaction);

            transactionRepository.save(transaction);
            log.debug("End service transaction: {}", transactionCode);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error processing service transaction", e);
        }
    }

    @Override
    public void cancelTransaction(Long transactionId) {

    }
}
