package dev.enrique.bank.service.impl;

import static dev.enrique.bank.commons.constants.ErrorMessage.TRANSACTION_NOT_FOUND;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.enrique.bank.client.AccountClient;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.exception.AccountServiceException;
import dev.enrique.bank.commons.exception.ApiRequestException;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.dto.request.AccountTransferRequest;
import dev.enrique.bank.dto.request.TransferRequest;
import dev.enrique.bank.dto.response.AccountTransferResponse;
import dev.enrique.bank.model.Transaction;
import dev.enrique.bank.service.TransactionCreationService;
import dev.enrique.bank.service.util.BasicMapper;
import dev.enrique.bank.service.util.TransferHelper;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TransactionCreationServiceImpl implements TransactionCreationService {
    private final AccountClient accountClient;
    private final TransferHelper transferHelper;
    private final TransactionRepository transactionRepository;
    private final BasicMapper basicMapper;

    @Override
    public void transferBetweenAccounts(TransferRequest transferRequest) {
        String transactionCode = UUID.randomUUID().toString();

        try {
            log.debug("Start transaction: {}", transactionCode);

            AccountTransferRequest request = basicMapper.mapTo(transferRequest,
                    AccountTransferRequest.class);

            AccountTransferResponse response = accountClient.processTransfer(request);

            Transaction transaction = transferHelper.buildTransfer(
                    transferRequest,
                    transactionCode,
                    response.transactionStatus(),
                    response.reason());

            transactionRepository.save(transaction);
            log.debug("End transaction {}", transactionCode);
        } catch (Exception e) {
            log.error("Unexpected error during transfer {}: {}", transactionCode, e.getMessage(), e);

            Transaction failedTransaction = transferHelper.buildTransfer(
                    transferRequest,
                    transactionCode,
                    TransactionStatus.FAILED,
                    "Unexpected error: " + e.getMessage());

            transactionRepository.save(failedTransaction);
            throw new RuntimeException("Unexpected error processing transfer", e);
        }
    }

    // This method needs implement validation based on TransferLimit
    @Override
    @Transactional
    public void createTransaction(String sourceAccountNumber, String targetAccountNumber, BigDecimal amount) {
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
