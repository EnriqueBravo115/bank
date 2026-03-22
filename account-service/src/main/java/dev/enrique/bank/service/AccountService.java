package dev.enrique.bank.service;

import dev.enrique.bank.broker.event.CreateAccountEvent;
import dev.enrique.bank.commons.dto.response.AccountDetailedResponse;

public interface AccountService {
    public void createAccount(CreateAccountEvent createAccountEvent, String authId);

    public void updateEmail(String accountNumber, String newEmail);

    public AccountDetailedResponse findByAccountNumber(String accountNumber);

    public AccountDetailedResponse findByClabe(String clabe);

    public AccountDetailedResponse findByEmail(String email);
}
