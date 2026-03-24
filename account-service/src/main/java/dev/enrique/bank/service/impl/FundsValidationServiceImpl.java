package dev.enrique.bank.service.impl;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import dev.enrique.bank.commons.enums.BalanceType;
import dev.enrique.bank.commons.enums.LimitType;
import dev.enrique.bank.commons.enums.TimePeriod;
import dev.enrique.bank.commons.exception.AccountNotFoundException;
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
    public Boolean hasSufficientFunds(Long accountId, BigDecimal amount) {
        AccountBalance latest = accountBalanceRepository
                .findLatestByAccountId(accountId)
                .orElseThrow(() -> new AccountNotFoundException(String.valueOf(accountId)));

        return latest.getBalance().compareTo(amount) >= 0;
    }

    @Override
    public BigDecimal getAvailableBalance(Long accountId) {
        AccountBalance latest = accountBalanceRepository
                .findLatestByAccountId(accountId)
                .orElseThrow(() -> new AccountNotFoundException(String.valueOf(accountId)));
        return latest.getBalance();
    }

    @Override
    public Boolean hasSufficientFundsIncludingHolds(Long accountId, BigDecimal amount) {
        BigDecimal available = accountBalanceRepository
                .findLatestByAccountIdAndType(accountId, BalanceType.AVAILABLE)
                .map(AccountBalance::getBalance)
                .orElseThrow(() -> new AccountNotFoundException(String.valueOf(accountId)));

        BigDecimal onHold = accountBalanceRepository
                .findLatestByAccountIdAndType(accountId, BalanceType.ON_HOLD)
                .map(AccountBalance::getBalance)
                .orElse(BigDecimal.ZERO);

        BigDecimal effectiveBalance = available.subtract(onHold);
        return effectiveBalance.compareTo(amount) >= 0;
    }

    @Override
    public Boolean isWithinDailyLimit(Long accountId, BigDecimal amount, LimitType limitType) {
        return transactionLimitRepository
                .findLatestByAccountIdAndLimitTypeAndTimePeriod(accountId, limitType, TimePeriod.DAILY)
                .map(limit -> amount.compareTo(limit.getMaxAmount()) <= 0)
                .orElse(true);
    }

    @Override
    public Boolean isWithinTransactionLimit(Long accountId, BigDecimal amount, LimitType limitType) {
        return transactionLimitRepository
                .findLatestByAccountIdAndLimitType(accountId, limitType)
                .map(limit -> amount.compareTo(limit.getMaxAmount()) <= 0)
                .orElse(true);
    }
}
