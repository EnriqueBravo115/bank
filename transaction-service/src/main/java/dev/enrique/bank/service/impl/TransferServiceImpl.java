package dev.enrique.bank.service.impl;

import static dev.enrique.bank.commons.constants.ErrorMessage.ACCOUNT_NOT_FOUND;
import static dev.enrique.bank.commons.constants.ErrorMessage.INSUFFICIENT_FUNDS;
import static dev.enrique.bank.commons.constants.ErrorMessage.TRANSACTION_NOT_FOUND;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import dev.enrique.bank.client.AccountClient;
import dev.enrique.bank.dto.response.AccountResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.commons.exception.ApiRequestException;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.model.Transaction;
import dev.enrique.bank.service.TransferService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {
    private final AccountClient accountClient;
    private final TransactionRepository transactionRepository;

    // This method needs implement validation based on TransferLimit
    @Override
    @Transactional
    public void transfer(String sourceAccountNumber, String targetAccountNumber, BigDecimal amount) {
        AccountResponse sourceAccount = accountClient.getAccount(sourceAccountNumber);
        AccountResponse targetAccount = accountClient.getAccount(targetAccountNumber);

        if (sourceAccount.getBalance().compareTo(amount) < 0)
            throw new ApiRequestException(INSUFFICIENT_FUNDS, HttpStatus.UNPROCESSABLE_ENTITY);

        sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
        targetAccount.setBalance(targetAccount.getBalance().add(amount));

        Transaction transaction = Transaction.builder()
                .amount(amount)
                .sourceAccountNumber(sourceAccountNumber)
                .targetAccountNumber(targetAccountNumber)
                .transactionType(TransactionType.TRANSFER)
                .description("Transfer between accounts")
                .transactionDate(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public void reverseTransfer(Long transactionId) {
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
