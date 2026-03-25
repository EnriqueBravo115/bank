package dev.enrique.bank.service.impl;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import dev.enrique.bank.commons.enums.BalanceType;
import dev.enrique.bank.commons.enums.LimitType;
import dev.enrique.bank.commons.enums.TimePeriod;
import dev.enrique.bank.commons.exception.AccountBalanceNotFoundException;
import dev.enrique.bank.dao.AccountBalanceRepository;
import dev.enrique.bank.dao.TransactionLimitRepository;
import dev.enrique.bank.model.AccountBalance;
import dev.enrique.bank.service.FundsValidationService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FundsValidationServiceImpl implements FundsValidationService {
    private final AccountBalanceRepository accountBalanceRepository;
    private final TransactionLimitRepository transactionLimitRepository;

    @Override
    public Boolean hasSufficientFunds(String accountNumber, BigDecimal amount) {
        AccountBalance latest = accountBalanceRepository
                .findLatestByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountBalanceNotFoundException(accountNumber));
        return latest.getBalance().compareTo(amount) >= 0;
    }

    @Override
    public BigDecimal getAvailableBalance(String accountNumber) {
        AccountBalance latest = accountBalanceRepository
                .findLatestByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountBalanceNotFoundException(accountNumber));
        return latest.getBalance();
    }

    @Override
    public Boolean hasSufficientFundsIncludingHolds(String accountNumber, BigDecimal amount) {
        BigDecimal available = accountBalanceRepository
                .findLatestByAccountNumberAndType(accountNumber, BalanceType.AVAILABLE)
                .map(AccountBalance::getBalance)
                .orElseThrow(() -> new AccountBalanceNotFoundException(accountNumber));

        BigDecimal onHold = accountBalanceRepository
                .findLatestByAccountNumberAndType(accountNumber, BalanceType.ON_HOLD)
                .map(AccountBalance::getBalance)
                .orElse(BigDecimal.ZERO);

        return available.subtract(onHold).compareTo(amount) >= 0;
    }

    @Override
    public Boolean isWithinDailyLimit(String accountNumber, BigDecimal amount, LimitType limitType) {
        return transactionLimitRepository
                .findLatestByAccountNumberAndLimitTypeAndTimePeriod(accountNumber, limitType, TimePeriod.DAILY)
                .map(limit -> amount.compareTo(limit.getMaxAmount()) <= 0)
                .orElse(true);
    }

    @Override
    public Boolean isWithinTransactionLimit(String accountNumber, BigDecimal amount, LimitType limitType) {
        return transactionLimitRepository
                .findLatestByAccountNumberAndLimitType(accountNumber, limitType)
                .map(limit -> amount.compareTo(limit.getMaxAmount()) <= 0)
                .orElse(true);
    }
}
