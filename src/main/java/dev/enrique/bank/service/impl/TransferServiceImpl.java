package dev.enrique.bank.service.impl;

import static dev.enrique.bank.service.util.TransactionHelper.twoLevelGroupingBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Pageable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.enrique.bank.commons.enums.Status;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.commons.exception.AccountNotFoundException;
import dev.enrique.bank.commons.exception.TransactionNotFoundException;
import dev.enrique.bank.dao.AccountRepository;
import dev.enrique.bank.dao.ScheduledTransferRepository;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.dto.request.TransferRequest;
import dev.enrique.bank.event.AccountTrasferEvent;
import dev.enrique.bank.mapper.BasicMapper;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.model.Transaction;
import dev.enrique.bank.service.TransferService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final ScheduledTransferRepository scheduledTransferRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final BasicMapper basicMapper;

    @Override
    @Transactional
    public void transfer(Long sourceAccountId, Long targetAccountId, BigDecimal amount) {
        Account sourceAccount = accountRepository.findById(sourceAccountId)
                .orElseThrow(() -> new RuntimeException("Source account not found"));

        Account targetAccount = accountRepository.findById(targetAccountId)
                .orElseThrow(() -> new RuntimeException("Source account not found"));

        if (sourceAccount.getBalance().compareTo(amount) < 0)
            throw new RuntimeException("Insuficient funds");

        sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
        targetAccount.setBalance(targetAccount.getBalance().add(amount));

        applicationEventPublisher.publishEvent(
                new AccountTrasferEvent(amount, sourceAccount, targetAccount));
    }

    // Historial de transferencias agrupado en dos niveles(date -> amount),
    // en el ultimo nivel hay una lista de TransferRequest
    @Override
    public Map<String, Map<String, List<TransferRequest>>> getTransferHistory(Long accountId) {
        return transactionRepository.findAllByAccountId(accountId).stream()
                .collect(twoLevelGroupingBy(
                        t -> t.getTransactionDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        t -> t.getAmount().toString(),
                        t -> basicMapper.convertToResponse(t, TransferRequest.class)));
    }

    @Transactional
    @Override
    public void reverseTransfer(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with id: " + transactionId));

        if (transaction.getTransactionStatus() == TransactionStatus.REVERSED)
            throw new IllegalStateException("Transaction is already reversed");

        if (transaction.getTransactionStatus() != TransactionStatus.COMPLETED)
            throw new IllegalStateException("Only completed can be reversed");

        Account sourceAccount = transaction.getSourceAccount();
        Account targetAccount = transaction.getTargetAccount();

        if (sourceAccount.getStatus() != Status.OPEN || targetAccount.getStatus() != Status.OPEN)
            throw new IllegalStateException("Both accounts must be open to reverse the transaction");

        if (targetAccount.getBalance().compareTo(transaction.getAmount()) < 0)
            throw new IllegalStateException("Insufficient balance in target account to reverse transaction");

        Transaction reversal = Transaction.builder()
                .sourceAccount(targetAccount)
                .targetAccount(sourceAccount)
                .amount(transaction.getAmount())
                .transactionType(TransactionType.REVERSAL)
                .transactionStatus(TransactionStatus.PENDING)
                .originalTransaction(transaction)
                .build();

        sourceAccount.increaseBalance(transaction.getAmount());
        targetAccount.reduceBalance(transaction.getAmount());

        accountRepository.save(sourceAccount);
        accountRepository.save(targetAccount);

        transaction.setTransactionStatus(TransactionStatus.REVERSED);
        transactionRepository.save(transaction);
        transactionRepository.save(reversal);
    }

    @Override
    public boolean hasSufficientFunds(Long accountId, BigDecimal amount) {
        if (accountId == null)
            throw new IllegalArgumentException("Account ID cannot be null");

        if (amount == null)
            throw new IllegalArgumentException("Amount cannot be null");

        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Amount must be greater than zero");

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + accountId));

        if (account.getStatus() != Status.OPEN)
            throw new IllegalStateException("Account is not open");

        return account.getBalance().compareTo(amount) >= 0;
    }

    @Override
    public void scheduleTransfer(TransferRequest request, LocalDateTime scheduleDate) {
        if (request == null)
            throw new IllegalArgumentException("TransferRequest cannot be null");

        if (scheduleDate == null)
            throw new IllegalArgumentException("Schedule date cannot be null");

        if (scheduleDate.isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("Schedule date cannot be in the past");

        Account sourceAccount = accountRepository.findByAccountNumber(request.getSourceAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("Source account not found"));

        Account targetAccount = accountRepository.findByAccountNumber(request.getTargetAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("Target account not found"));

        // Validar estados de cuentas
        if (sourceAccount.getStatus() != Status.OPEN || targetAccount.getStatus() != Status.OPEN) {
            throw new IllegalStateException("Both accounts must be open");
        }

        // Validar fondos suficientes (si es una transferencia inmediata)
        if (request.isImmediate() && sourceAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds in source account");
        }

        // Crear la transacción programada
        ScheduledTransfer scheduledTransfer = ScheduledTransfer.builder()
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .amount(request.getAmount())
                .description(request.getDescription())
                .scheduledDate(scheduleDate)
                .status(ScheduledTransferStatus.PENDING)
                .creationDate(LocalDateTime.now())
                .build();

        scheduledTransferRepository.save(scheduledTransfer);

        scheduleTransferExecution(scheduledTransfer);
    }

    @Override
    public Transaction getTransferDetails(Long transactionId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTransferDetails'");
    }

    @Override
    public void cancelScheduledTransfer(Long transactionId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'cancelScheduledTransfer'");
    }

    @Override
    public void transferBetweenUsers(Long sourceId, Long targetUserId, BigDecimal amount) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'transferBetweenUsers'");
    }

    @Override
    public BigDecimal calculateTransferFee(BigDecimal amount, String currency) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'calculateTransferFee'");
    }

    @Override
    public void urgentTransfer(TransferRequest request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'urgentTransfer'");
    }

    @Override
    public BigDecimal getTransferLimit(Long accountId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTransferLimit'");
    }

    @Override
    public void notifyTransfer(Long transactionId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'notifyTransfer'");
    }

    @Override
    public void validateTransfer(TransferRequest request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'validateTransfer'");
    }

    @Override
    public Page<Transaction> getAllTransfers(Pageable pageable) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAllTransfers'");
    }
}
