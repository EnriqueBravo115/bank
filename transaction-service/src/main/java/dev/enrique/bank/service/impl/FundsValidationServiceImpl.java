package dev.enrique.bank.service.impl;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import dev.enrique.bank.model.Account;
import dev.enrique.bank.service.FundsValidationService;
import dev.enrique.bank.service.util.AccountHelper;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class FundsValidationServiceImpl implements FundsValidationService {
    private final AccountHelper accountHelper;

    @Override
    public Boolean hasSufficientFunds(Long accountId, BigDecimal amount) {
        Account account = accountHelper.getAccountById(accountId);
        return account.getBalance().compareTo(amount) > 0;
    }

    @Override
    public BigDecimal getAvailableBalance(Long accountId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAvailableBalance'");
    }

    @Override
    public Boolean hasSufficientFundsIncludingHolds(Long accountId, BigDecimal amount) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'hasSufficientFundsIncludingHolds'");
    }

    @Override
    public Boolean isWithinDailyLimit(Long accountId, BigDecimal amount) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isWithinDailyLimit'");
    }

    @Override
    public Boolean isWithinTransactionLimit(Long accountId, BigDecimal amount) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isWithinTransactionLimit'");
    }
}
