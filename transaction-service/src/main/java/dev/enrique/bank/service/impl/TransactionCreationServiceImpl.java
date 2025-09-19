package dev.enrique.bank.service.impl;

import static dev.enrique.bank.commons.constants.ErrorMessage.TRANSACTION_NOT_FOUND;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.enrique.bank.client.AccountClient;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.commons.exception.ApiRequestException;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.dto.request.AccountTransferRequest;
import dev.enrique.bank.dto.response.AccountResponse;
import dev.enrique.bank.model.Transaction;
import dev.enrique.bank.service.TransactionCreationService;
import dev.enrique.bank.service.util.TransferHelper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionCreationServiceImpl implements TransactionCreationService {
    private final AccountClient accountClient;
    private final TransactionRepository transactionRepository;

    @Override
    public void transferBetweenAccounts(AccountTransferRequest accountTransferRequest) {

    }

    // This method needs implement validation based on TransferLimit
    @Override
    @Transactional
    public void createTransaction(String sourceAccountNumber, String targetAccountNumber, BigDecimal amount) {
        AccountResponse sourceAccountResponse = accountClient.validateTransaction(sourceAccountNumber);
        AccountResponse targetAccountResponse = accountClient.validateTransaction(targetAccountNumber);

        sourceAccountResponse.setBalance(sourceAccountResponse.getBalance().subtract(amount));
        targetAccountResponse.setBalance(targetAccountResponse.getBalance().add(amount));

        accountClient.updateAccountBalance(sourceAccountNumber, sourceAccountResponse);
        accountClient.updateAccountBalance(targetAccountNumber, targetAccountResponse);

        Transaction transaction = Transaction.builder()
                .amount(amount)
                .transactionCode(UUID.randomUUID().toString())
                .sourceAccountNumber(sourceAccountNumber)
                .targetAccountNumber(targetAccountNumber)
                .transactionType(TransactionType.TRANSFER)
                .transactionStatus(TransactionStatus.COMPLETED)
                .description("Transfer between accounts")
                .transactionDate(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);
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
