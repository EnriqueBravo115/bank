package dev.enrique.bank.service.impl;

import org.springframework.stereotype.Service;

import dev.enrique.bank.broker.event.CreateAccountEvent;
import dev.enrique.bank.commons.dto.response.AccountDetailedResponse;
import dev.enrique.bank.commons.dto.response.AccountUpdatedResponse;
import dev.enrique.bank.commons.enums.AccountStatus;
import dev.enrique.bank.commons.exception.AccountAlreadyExistsException;
import dev.enrique.bank.commons.exception.AccountNotFoundException;
import dev.enrique.bank.commons.util.ClabeGenerator;
import dev.enrique.bank.dao.AccountRepository;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.service.AccountService;
import jakarta.transaction.Transactional;
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

    @Override
    @Transactional
    public AccountUpdatedResponse updateEmail(String accountNumber, String newEmail) {
        if (accountRepository.existsByEmail(newEmail)) {
            throw new AccountAlreadyExistsException("email", newEmail);
        }

        Account account = accountRepository
                .findEntityByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        account.setEmail(newEmail);
        accountRepository.save(account);

        return new AccountUpdatedResponse(
                account.getId(),
                account.getAccountNumber(),
                "email",
                newEmail);
    }

    @Override
    public AccountDetailedResponse findByAccountNumber(String accountNumber) {
        return accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    @Override
    public AccountDetailedResponse findByClabe(String clabe) {
        return accountRepository
                .findByClabe(clabe)
                .orElseThrow(() -> new AccountNotFoundException(clabe));
    }

    @Override
    public AccountDetailedResponse findByEmail(String email) {
        return accountRepository
                .findByEmail(email)
                .orElseThrow(() -> new AccountNotFoundException(email));
    }

    private String generateUniqueClabe() {
        String clabe;
        do {
            clabe = clabeGenerator.generate();
        } while (accountRepository.existsByClabe(clabe));
        return clabe;
    }
}
