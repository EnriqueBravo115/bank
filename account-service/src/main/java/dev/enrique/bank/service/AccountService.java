package dev.enrique.bank.service;

import dev.enrique.bank.broker.event.CreateAccountEvent;
import dev.enrique.bank.commons.dto.response.AccountDetailedResponse;

public interface AccountService {
    public void createAccount(CreateAccountEvent createAccountEvent, String authId);

    public void updateStatusByUserId(String userId, boolean userActive);

    public void updateEmailByUserId(String userId, String newEmail);

    public AccountDetailedResponse findByAccountNumber(String accountNumber);

    public AccountDetailedResponse findByClabe(String clabe);

    public AccountDetailedResponse findByEmail(String email);

    public AccountDetailedResponse findByUserId(String userId);
}
