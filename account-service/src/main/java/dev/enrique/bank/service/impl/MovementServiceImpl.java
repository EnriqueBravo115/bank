package dev.enrique.bank.service.impl;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import dev.enrique.bank.commons.dto.request.ClientPurchaseRequest;
import dev.enrique.bank.commons.dto.request.ClientServiceRequest;
import dev.enrique.bank.commons.dto.request.ClientTransferRequest;
import dev.enrique.bank.commons.dto.request.ClientWithdrawalRequest;
import dev.enrique.bank.commons.dto.response.MovementResultResponse;
import dev.enrique.bank.commons.enums.BalanceType;
import dev.enrique.bank.commons.enums.LimitType;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.exception.AccountBalanceNotFoundException;
import dev.enrique.bank.commons.exception.AccountNotFoundException;
import dev.enrique.bank.commons.exception.InsufficientFundsException;
import dev.enrique.bank.commons.exception.TransactionLimitExceededException;
import dev.enrique.bank.dao.AccountBalanceRepository;
import dev.enrique.bank.dao.AccountRepository;
import dev.enrique.bank.model.AccountBalance;
import dev.enrique.bank.service.FundsValidationService;
import dev.enrique.bank.service.MovementService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovementServiceImpl implements MovementService {
    private final FundsValidationService fundsValidationService;
    private final AccountBalanceRepository accountBalanceRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public MovementResultResponse processTransfer(ClientTransferRequest request) {
        validate(request.sourceAccountNumber(), request.amount(), LimitType.TRANSFER, false);
        transfer(request.sourceAccountNumber(), request.targetAccountNumber(), request.amount());
        return new MovementResultResponse(TransactionStatus.COMPLETED, "Valid Transaction");
    }

    @Override
    @Transactional
    public MovementResultResponse processPurchase(ClientPurchaseRequest request) {
        validate(request.accountNumber(), request.amount(), LimitType.PURCHASE, false);
        debit(request.accountNumber(), request.amount());
        return new MovementResultResponse(TransactionStatus.COMPLETED, "Valid Transaction");
    }

    @Override
    @Transactional
    public MovementResultResponse processService(ClientServiceRequest request) {
        validate(request.accountNumber(), request.amount(), LimitType.SERVICE, false);
        debit(request.accountNumber(), request.amount());
        return new MovementResultResponse(TransactionStatus.COMPLETED, "Valid Transaction");
    }

    @Override
    @Transactional
    public MovementResultResponse processWithdrawal(ClientWithdrawalRequest request) {
        validate(request.accountNumber(), request.amount(), LimitType.WITHDRAWAL, false);
        debit(request.accountNumber(), request.amount());
        return new MovementResultResponse(TransactionStatus.COMPLETED, "Valid Transaction");
    }

    private void validate(String accountNumber, BigDecimal amount,
            LimitType limitType, boolean checkHolds) {
        if (!accountRepository.existsByAccountNumber(accountNumber)) {
            throw new AccountNotFoundException(accountNumber);
        }

        if (!fundsValidationService.hasSufficientFunds(accountNumber, amount)) {
            throw new InsufficientFundsException(accountNumber, amount);
        }

        if (checkHolds && !fundsValidationService.hasSufficientFundsIncludingHolds(accountNumber, amount)) {
            throw new InsufficientFundsException(accountNumber, amount);
        }

        if (!fundsValidationService.isWithinTransactionLimit(accountNumber, amount, limitType)) {
            throw new TransactionLimitExceededException("Amount exceeds the allowed transaction limit");
        }

        if (!fundsValidationService.isWithinDailyLimit(accountNumber, amount, limitType)) {
            throw new TransactionLimitExceededException("Amount exceeds the allowed transaction limit");
        }
    }

    private void debit(String accountNumber, BigDecimal amount) {
        AccountBalance latest = accountBalanceRepository
                .findLatestByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountBalanceNotFoundException(accountNumber));

        accountBalanceRepository.save(AccountBalance.builder()
                .account(latest.getAccount())
                .balance(latest.getBalance().subtract(amount))
                .balanceType(BalanceType.INTRADAY)
                .build());
    }

    private void transfer(String sourceAccountNumber, String targetAccountNumber, BigDecimal amount) {
        AccountBalance latestSource = accountBalanceRepository
                .findLatestByAccountNumber(sourceAccountNumber)
                .orElseThrow(() -> new AccountBalanceNotFoundException(sourceAccountNumber));

        accountBalanceRepository.save(AccountBalance.builder()
                .account(latestSource.getAccount())
                .balance(latestSource.getBalance().subtract(amount))
                .balanceType(BalanceType.INTRADAY)
                .build());

        AccountBalance latestTarget = accountBalanceRepository
                .findLatestByAccountNumber(targetAccountNumber)
                .orElseThrow(() -> new AccountBalanceNotFoundException(targetAccountNumber));

        accountBalanceRepository.save(AccountBalance.builder()
                .account(latestTarget.getAccount())
                .balance(latestTarget.getBalance().add(amount))
                .balanceType(BalanceType.INTRADAY)
                .build());
    }
}
