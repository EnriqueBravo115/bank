package dev.enrique.bank.service;

import dev.enrique.bank.broker.event.CreateAccountEvent;

public interface AccountService {
    public void createAccount(CreateAccountEvent createAccountEvent, String authId);
}
