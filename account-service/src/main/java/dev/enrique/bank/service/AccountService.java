package dev.enrique.bank.service;

import dev.enrique.bank.broker.event.CreateAccountEvent;
import dev.enrique.bank.commons.dto.response.AccountDetailedResponse;

public interface AccountService {
    public void createAccount(CreateAccountEvent createAccountEvent, String authId);

    AccountDetailedResponse findByAccountNumber(String accountNumber);
}
