package dev.enrique.bank.service.impl;

import static dev.enrique.bank.constants.ErrorMessage.ACCOUNT_NOT_FOUND;
import static dev.enrique.bank.constants.ErrorMessage.INSUFFICIENT_FUNDS;
import static dev.enrique.bank.constants.ErrorMessage.TRANSACTION_NOT_FOUND;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.enrique.bank.enums.TransactionStatus;
import dev.enrique.bank.enums.TransactionType;
import dev.enrique.bank.exception.ApiRequestException;
import dev.enrique.bank.dao.AccountRepository;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.model.Transaction;
import dev.enrique.bank.service.TransferService;
//import dev.enrique.bank.service.util.TransferHelper;
import lombok.RequiredArgsConstructor;

//@Service
//@RequiredArgsConstructor
//public class TransferServiceImpl implements TransferService {
//    private final TransactionRepository transactionRepository;
//    private final AccountRepository accountRepository;
//    private final TransferHelper transferHelper;
//
//    @Override
//    @Transactional
//    public void transfer(Long sourceAccountId, Long targetAccountId, BigDecimal amount) {
//        Account sourceAccount = accountRepository.findById(sourceAccountId)
//                .orElseThrow(() -> new ApiRequestException(ACCOUNT_NOT_FOUND, HttpStatus.NOT_FOUND));
//
//        Account targetAccount = accountRepository.findById(targetAccountId)
//                .orElseThrow(() -> new ApiRequestException(ACCOUNT_NOT_FOUND, HttpStatus.NOT_FOUND));
//
//        if (sourceAccount.getBalance().compareTo(amount) < 0)
//            throw new ApiRequestException(INSUFFICIENT_FUNDS, HttpStatus.UNPROCESSABLE_ENTITY);
//
//        sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
//        targetAccount.setBalance(targetAccount.getBalance().add(amount));
//
//        Transaction transaction = Transaction.builder()
//                .amount(amount)
//                .sourceAccount(sourceAccount)
//                .targetAccount(targetAccount)
//                .transactionType(TransactionType.TRANSFER)
//                .description("Transfer between accounts")
//                .transactionDate(LocalDateTime.now())
//                .build();
//
//        transactionRepository.save(transaction);
//    }
//
//    @Override
//    @Transactional
//    public void reverseTransfer(Long transactionId) {
//        Transaction transaction = transactionRepository.findById(transactionId)
//                .orElseThrow(() -> new ApiRequestException(TRANSACTION_NOT_FOUND, HttpStatus.NOT_FOUND));
//
//        Account sourceAccount = transaction.getSourceAccount();
//        Account targetAccount = transaction.getTargetAccount();
//
//        transferHelper.validateReverseTransfer(transaction, sourceAccount, targetAccount);
//
//        Transaction reversal = Transaction.builder()
//                .sourceAccount(targetAccount)
//                .targetAccount(sourceAccount)
//                .amount(transaction.getAmount())
//                .transactionType(TransactionType.REVERSAL)
//                .transactionStatus(TransactionStatus.PENDING)
//                .originalTransaction(transaction)
//                .build();
//
//        sourceAccount.setBalance(sourceAccount.getBalance().add(transaction.getAmount()));
//        targetAccount.setBalance(targetAccount.getBalance().subtract(transaction.getAmount()));
//
//        transaction.setTransactionStatus(TransactionStatus.REVERSED);
//        transactionRepository.save(reversal);
//    }
//}
//