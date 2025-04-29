package dev.enrique.bank.service.impl;

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

import dev.enrique.bank.dao.AccountRepository;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.dto.request.TransferRequest;
import dev.enrique.bank.event.AccountTrasferEvent;
import dev.enrique.bank.mapper.BasicMapper;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.model.Transaction;
import dev.enrique.bank.service.TransferService;
import lombok.RequiredArgsConstructor;

import static dev.enrique.bank.service.util.TransactionHelper.*;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
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

    @Override
    public void reverseTrasfer(Long transactionId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'reverseTrasfer'");
    }

    @Override
    public boolean hasSuffiicientFunds(Long accountId, BigDecimal amount) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'hasSuffiicientFunds'");
    }

    @Override
    public void scheduleTransfer(TransferRequest request, LocalDateTime scheduleDate) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'scheduleTransfer'");
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
