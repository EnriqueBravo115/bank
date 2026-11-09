package dev.enrique.bank.service.impl;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

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
}
