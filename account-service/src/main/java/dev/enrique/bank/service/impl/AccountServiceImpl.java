package dev.enrique.bank.service.impl;

import org.springframework.stereotype.Service;

import dev.enrique.bank.broker.event.CreateAccountEvent;
import java.util.UUID;

import dev.enrique.bank.commons.enums.AccountStatus;
import dev.enrique.bank.commons.enums.AccountType;
import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.dao.AccountRepository;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.service.AccountService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;

    @Override
    public void createAccount(CreateAccountEvent createAccountEvent, String authId) {
        Account account = new Account();
        account.setUserId(authId);
        account.setClabe("123456789123456789");
        account.setAccountType(AccountType.SAVING);
        account.setAccountNumber("ACC-" + System.currentTimeMillis());
        account.setAccountStatus(AccountStatus.OPEN);
        account.setCurrency(Currency.MX);

        accountRepository.save(account);
    }

}
