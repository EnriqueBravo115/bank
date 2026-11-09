package dev.enrique.bank.service.impl;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import dev.enrique.bank.commons.enums.BalanceType;
import dev.enrique.bank.commons.exception.AccountBalanceNotFoundException;
import dev.enrique.bank.dao.AccountBalanceRepository;
import dev.enrique.bank.model.AccountBalance;
import dev.enrique.bank.service.FundsValidationService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FundsValidationServiceImpl implements FundsValidationService {
    private final AccountBalanceRepository accountBalanceRepository;

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
}
