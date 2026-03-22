package dev.enrique.bank.service.impl;

import org.springframework.stereotype.Service;

import dev.enrique.bank.broker.event.CreateAccountEvent;
import dev.enrique.bank.commons.enums.AccountStatus;
import dev.enrique.bank.commons.util.ClabeGenerator;
import dev.enrique.bank.dao.AccountRepository;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.service.AccountService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final ClabeGenerator clabeGenerator;

    @Override
    public void createAccount(CreateAccountEvent createAccountEvent, String authId) {
        Account account = new Account();
        account.setUserId(authId);
        account.setEmail(createAccountEvent.getEmail());
        account.setAccountType(createAccountEvent.getAccountType());
        account.setCurrency(createAccountEvent.getCurrency());
        account.setClabe(generateUniqueClabe());
        account.setAccountNumber("ACC-" + System.currentTimeMillis());
        account.setAccountStatus(AccountStatus.OPEN);

        accountRepository.save(account);
    }

    private String generateUniqueClabe() {
        String clabe;
        do {
            clabe = clabeGenerator.generate();
        } while (accountRepository.existsByClabe(clabe));
        return clabe;
    }
}
