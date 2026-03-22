package dev.enrique.bank.service;

import dev.enrique.bank.broker.event.CreateAccountEvent;
import dev.enrique.bank.commons.dto.response.AccountDetailedResponse;
import dev.enrique.bank.commons.dto.response.AccountUpdatedResponse;

public interface AccountService {
    public void createAccount(CreateAccountEvent createAccountEvent, String authId);

    AccountUpdatedResponse updateEmail(String accountNumber, String newEmail);

    AccountDetailedResponse findByAccountNumber(String accountNumber);

    AccountDetailedResponse findByClabe(String clabe);

    AccountDetailedResponse findByEmail(String email);
}
