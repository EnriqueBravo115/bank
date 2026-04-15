package dev.enrique.bank.service;

import dev.enrique.bank.commons.dto.request.CreateBalanceRequest;
import dev.enrique.bank.commons.dto.response.AccountBalanceResponse;

public interface AccountBalanceService {
    AccountBalanceResponse createAccountBalance(Long accountId, CreateBalanceRequest balanceRequest);
}
