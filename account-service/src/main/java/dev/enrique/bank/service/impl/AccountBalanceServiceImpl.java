package dev.enrique.bank.service.impl;

import org.springframework.stereotype.Service;

import dev.enrique.bank.commons.dto.request.CreateBalanceRequest;
import dev.enrique.bank.commons.dto.response.AccountBalanceResponse;
import dev.enrique.bank.service.AccountBalanceService;

@Service
public class AccountBalanceServiceImpl implements AccountBalanceService {
    @Override
    public AccountBalanceResponse createAccountBalance(Long accountId, CreateBalanceRequest balanceRequest) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createAccountBalance'");
    }
}
