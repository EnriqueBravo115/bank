package dev.enrique.bank.service.impl;

import static dev.enrique.bank.commons.constants.ErrorMessage.TRANSACTION_NOT_FOUND;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.enrique.bank.client.AccountClient;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.commons.exception.ApiRequestException;
import dev.enrique.bank.commons.util.BasicMapper;
import dev.enrique.bank.commons.util.TransactionFactoryProvider;
import dev.enrique.bank.commons.util.TransactionHelper;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.model.TransferTransaction;
import dev.enrique.bank.commons.dto.request.AccountPurchaseRequest;
import dev.enrique.bank.commons.dto.request.AccountTransferRequest;
import dev.enrique.bank.commons.dto.request.PurchaseRequest;
import dev.enrique.bank.commons.dto.request.ServiceRequest;
import dev.enrique.bank.commons.dto.request.TransferRequest;
import dev.enrique.bank.commons.dto.response.MovementResultResponse;
import dev.enrique.bank.service.TransactionCreationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// FIX: refactor models to manage more cases from TRANSACTION to especific operations
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TransactionCreationServiceImpl implements TransactionCreationService {
    private final AccountClient accountClient;
    private final TransactionRepository transactionRepository;
    private final TransactionHelper transactionHelper;
    private final BasicMapper basicMapper;

    @Override
    public void transfer(TransferRequest transferRequest) {
        String transactionCode = UUID.randomUUID().toString();

        try {
            log.debug("Start transfer transaction: {}", transactionCode);

            AccountTransferRequest request = basicMapper.mapTo(transferRequest, AccountTransferRequest.class);
            MovementResultResponse response = accountClient.processTransfer(request);

            TransferTransaction transferTransaction = TransferTransaction.builder()
                    .transactionCode(transactionCode)
                    .amount(transferRequest.getAmount())
                    .description(transferRequest.getDescription())
                    .reason(response.reason())
                    .transactionDate(LocalDateTime.now())
                    .currency(transferRequest.getCurrency())
                    .transactionType(TransactionType.TRANSFER)
                    .transactionStatus(response.transactionStatus())
                    .sourceAccountNumber(transferRequest.getSourceAccountNumber())
                    .targetAccountNumber(transferRequest.getTargetAccountNumber())
                    .build();

            log.debug("End transfer transaction: {}", transactionCode);
        } catch (Exception e) {
            transactionRepository.save(failedTransaction);
            throw new RuntimeException("Unexpected error processing transfer", e);
        }
    }

    @Override
    public void purchase(PurchaseRequest purchaseRequest) {
        String transactionCode = UUID.randomUUID().toString();

        try {
            log.debug("Start purchase transaction: {}", transactionCode);
            AccountPurchaseRequest request = basicMapper.mapTo(purchaseRequest, AccountPurchaseRequest.class);
            MovementResultResponse response = accountClient.processPurchase(request);

            Transaction transaction = transactionHelper.buildPurchase(
                    purchaseRequest,
                    transactionCode,
                    response.transactionStatus(),
                    response.reason());

            transactionRepository.save(transaction);
            log.debug("End purchase transaction: {}", transactionCode);
        } catch (Exception e) {
            log.error("Unexpected error during transfer {}: {}", transactionCode, e.getMessage());

            Transaction failedTransaction = transactionHelper.buildTransfer(
                    purchaseRequest,
                    transactionCode,
                    TransactionStatus.FAILED,
                    "Unexpected error: " + e.getMessage());

            throw new RuntimeException("Unexpected error processing transfer", e);
        }
    }

    @Override
    public void service(ServiceRequest purchaseRequest) {

    }

    @Override
    public void cancelTransaction(Long transactionId) {

    }

    @Override
    @Transactional
    public void reverseTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ApiRequestException(TRANSACTION_NOT_FOUND, HttpStatus.NOT_FOUND));

        AccountResponse sourceAccount = accountClient.getAccount(transaction.getSourceAccountNumber());
        AccountResponse targetAccount = accountClient.getAccount(transaction.getTargetAccountNumber());

        Transaction reversal = Transaction.builder()
                .sourceAccountNumber(transaction.getTargetAccountNumber())
                .targetAccountNumber(transaction.getSourceAccountNumber())
                .amount(transaction.getAmount())
                .transactionType(transaction.getTransactionType())
                .transactionStatus(TransactionStatus.REFUND)
                .originalTransaction(transaction)
                .build();

        sourceAccount.setBalance(sourceAccount.getBalance().add(transaction.getAmount()));
        targetAccount.setBalance(targetAccount.getBalance().subtract(transaction.getAmount()));

        transaction.setTransactionStatus(TransactionStatus.REVERSED);
        transactionRepository.save(reversal);
    }
}
