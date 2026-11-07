package dev.enrique.bank.util;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dev.enrique.bank.commons.enums.AccountStatus;
import dev.enrique.bank.commons.enums.AccountType;
import dev.enrique.bank.commons.enums.BalanceType;
import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.dao.AccountBalanceRepository;
import dev.enrique.bank.dao.AccountRepository;
import dev.enrique.bank.dao.MovementRepository;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.model.AccountBalance;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

/** Creates and clears the accounts each test needs (picked up by component scan in tests). */
@Component
@RequiredArgsConstructor
public class AccountFixtures {
    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final MovementRepository movementRepository;
    private final EntityManager entityManager;

    @Transactional
    public void clear() {
        movementRepository.deleteAll();
        entityManager.createQuery("DELETE FROM AccountBalance").executeUpdate();
        entityManager.createQuery("DELETE FROM TransactionLimit").executeUpdate();
        entityManager.createQuery("DELETE FROM Account").executeUpdate();
    }

    @Transactional
    public Account account(String userId, String accountNumber, String clabe, AccountStatus status, String balance) {
        Account account = accountRepository.save(Account.builder()
                .userId(userId)
                .email(userId + "@test.com")
                .accountNumber(accountNumber)
                .clabe(clabe)
                .accountType(AccountType.CHECKING)
                .accountStatus(status)
                .currency(Currency.MX)
                .build());
        accountBalanceRepository.save(AccountBalance.builder()
                .account(account)
                .balance(new BigDecimal(balance))
                .balanceType(BalanceType.INTRADAY)
                .build());
        return account;
    }

    public BigDecimal balanceOf(String accountNumber) {
        return accountBalanceRepository.findLatestByAccountNumber(accountNumber).orElseThrow().getBalance();
    }

    public List<AccountBalance> balances() {
        return accountBalanceRepository.findAll();
    }
}
