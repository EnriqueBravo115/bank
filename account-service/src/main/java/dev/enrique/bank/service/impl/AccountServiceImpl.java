package dev.enrique.bank.service.impl;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import dev.enrique.bank.broker.event.CreateAccountEvent;
import dev.enrique.bank.commons.dto.response.AccountDetailedResponse;
import dev.enrique.bank.commons.enums.AccountStatus;
import dev.enrique.bank.commons.enums.BalanceType;
import dev.enrique.bank.commons.exception.AccountNotFoundException;
import dev.enrique.bank.commons.util.AccountNumberGenerator;
import dev.enrique.bank.commons.util.ClabeGenerator;
import dev.enrique.bank.dao.AccountBalanceRepository;
import dev.enrique.bank.dao.AccountRepository;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.model.AccountBalance;
import dev.enrique.bank.service.AccountService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final ClabeGenerator clabeGenerator;
    private final AccountNumberGenerator accountNumberGenerator;

    @Override
    @Transactional
    public void createAccount(CreateAccountEvent createAccountEvent, String authId) {
        // user-service publishes through an outbox (at-least-once), so the same request can arrive twice.
        if (accountRepository.existsByUserId(authId)) {
            log.info("Account already exists for user {}, ignoring repeated account request", authId);
            return;
        }

        Account account = new Account();
        account.setUserId(authId);
        account.setEmail(createAccountEvent.getEmail());
        account.setAccountType(createAccountEvent.getAccountType());
        account.setCurrency(createAccountEvent.getCurrency());
        account.setClabe(generateUniqueClabe());
        account.setAccountNumber(generateUniqueAccountNumber());
        account.setAccountStatus(AccountStatus.OPEN);

        accountRepository.save(account);

        // Every movement reads the latest balance row: without this one they all failed.
        accountBalanceRepository.save(AccountBalance.builder()
                .account(account)
                .balance(BigDecimal.ZERO)
                .balanceType(BalanceType.OPENING)
                .build());
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
                .orElseThrow(() -> new AccountNotFoundException("clabe", clabe));
    }

    @Override
    public AccountDetailedResponse findByEmail(String email) {
        return accountRepository
                .findByEmail(email)
                .orElseThrow(() -> new AccountNotFoundException("email", email));
    }

    @Override
    public AccountDetailedResponse findByUserId(String userId) {
        return accountRepository
                .findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException("userId", userId));
    }

    // Blocking a user freezes the account; unblocking only reopens an account that was frozen,
    // never one that was closed or marked inactive for other reasons. Repeated events are no-ops.
    @Override
    @Transactional
    public void updateStatusByUserId(String userId, boolean userActive) {
        accountRepository.findEntityByUserId(userId).ifPresentOrElse(account -> {
            if (!userActive && account.getAccountStatus() == AccountStatus.OPEN) {
                account.setAccountStatus(AccountStatus.FROZEN);
            } else if (userActive && account.getAccountStatus() == AccountStatus.FROZEN) {
                account.setAccountStatus(AccountStatus.OPEN);
            }
        }, () -> log.info("No account for user {}, ignoring status change", userId));
    }

    @Override
    @Transactional
    public void updateEmailByUserId(String userId, String newEmail) {
        accountRepository.findEntityByUserId(userId).ifPresentOrElse(
                account -> account.setEmail(newEmail),
                () -> log.info("No account for user {}, ignoring email change", userId));
    }

    private String generateUniqueAccountNumber() {
        String number;
        do {
            number = accountNumberGenerator.generate();
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }

    private String generateUniqueClabe() {
        String clabe;
        do {
            clabe = clabeGenerator.generate();
        } while (accountRepository.existsByClabe(clabe));
        return clabe;
    }
}
