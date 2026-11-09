package dev.enrique.bank.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.enrique.bank.broker.event.CreateAccountEvent;
import dev.enrique.bank.commons.enums.AccountStatus;
import dev.enrique.bank.commons.enums.AccountType;
import dev.enrique.bank.commons.enums.BalanceType;
import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.util.AccountNumberGenerator;
import dev.enrique.bank.commons.util.ClabeGenerator;
import dev.enrique.bank.dao.AccountBalanceRepository;
import dev.enrique.bank.dao.AccountRepository;
import dev.enrique.bank.dao.PendingUserUpdateRepository;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.model.AccountBalance;
import dev.enrique.bank.model.PendingUserUpdate;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {
    private static final String USER_ID = "keycloak-user-1";

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountBalanceRepository accountBalanceRepository;
    @Mock
    private ClabeGenerator clabeGenerator;
    @Mock
    private AccountNumberGenerator accountNumberGenerator;
    @Mock
    private PendingUserUpdateRepository pendingUserUpdateRepository;
    @InjectMocks
    private AccountServiceImpl accountService;

    @Test
    void shouldCreateAccountForNewUser() {
        when(accountRepository.existsByUserId(USER_ID)).thenReturn(false);
        when(clabeGenerator.generate()).thenReturn("012345678901234567");
        when(accountRepository.existsByClabe("012345678901234567")).thenReturn(false);
        // The first number is taken: another one is generated
        when(accountNumberGenerator.generate()).thenReturn("ACC-1111111111111111", "ACC-2222222222222222");
        when(accountRepository.existsByAccountNumber("ACC-1111111111111111")).thenReturn(true);

        accountService.createAccount(event(), USER_ID);

        ArgumentCaptor<Account> saved = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getValue().getEmail()).isEqualTo("user@test.com");
        assertThat(saved.getValue().getAccountType()).isEqualTo(AccountType.SAVING);
        assertThat(saved.getValue().getCurrency()).isEqualTo(Currency.MX);
        assertThat(saved.getValue().getAccountStatus()).isEqualTo(AccountStatus.OPEN);
        assertThat(saved.getValue().getAccountNumber()).isEqualTo("ACC-2222222222222222");

        ArgumentCaptor<AccountBalance> opening = ArgumentCaptor.forClass(AccountBalance.class);
        verify(accountBalanceRepository).save(opening.capture());
        assertThat(opening.getValue().getAccount()).isSameAs(saved.getValue());
        assertThat(opening.getValue().getBalance()).isEqualByComparingTo("0");
        assertThat(opening.getValue().getBalanceType()).isEqualTo(BalanceType.OPENING);
    }

    @Test
    void shouldIgnoreRepeatedAccountRequest() {
        when(accountRepository.existsByUserId(USER_ID)).thenReturn(true);

        accountService.createAccount(event(), USER_ID);

        verify(accountRepository, never()).save(any());
        verify(accountBalanceRepository, never()).save(any());
    }

    @Test
    void shouldFreezeOpenAccountWhenUserIsBlocked() {
        Account account = account(AccountStatus.OPEN);
        when(accountRepository.findEntityByUserId(USER_ID)).thenReturn(Optional.of(account));

        accountService.updateStatusByUserId(USER_ID, false);

        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.FROZEN);
    }

    @Test
    void shouldReopenFrozenAccountWhenUserIsUnblocked() {
        Account account = account(AccountStatus.FROZEN);
        when(accountRepository.findEntityByUserId(USER_ID)).thenReturn(Optional.of(account));

        accountService.updateStatusByUserId(USER_ID, true);

        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.OPEN);
    }

    @Test
    void shouldNotReopenClosedAccountWhenUserIsUnblocked() {
        Account account = account(AccountStatus.CLOSE);
        when(accountRepository.findEntityByUserId(USER_ID)).thenReturn(Optional.of(account));

        accountService.updateStatusByUserId(USER_ID, true);

        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.CLOSE);
    }

    @Test
    void shouldKeepStatusChangeForWhenTheAccountIsOpened() {
        when(accountRepository.findEntityByUserId(USER_ID)).thenReturn(Optional.empty());
        when(pendingUserUpdateRepository.findById(USER_ID)).thenReturn(Optional.empty());

        accountService.updateStatusByUserId(USER_ID, false);

        ArgumentCaptor<PendingUserUpdate> pending = ArgumentCaptor.forClass(PendingUserUpdate.class);
        verify(pendingUserUpdateRepository).save(pending.capture());
        assertThat(pending.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(pending.getValue().getActive()).isFalse();
        verify(accountRepository, never()).save(any());
    }

    @Test
    void shouldUpdateEmailOfUserAccount() {
        Account account = account(AccountStatus.OPEN);
        when(accountRepository.findEntityByUserId(USER_ID)).thenReturn(Optional.of(account));

        accountService.updateEmailByUserId(USER_ID, "new@test.com");

        assertThat(account.getEmail()).isEqualTo("new@test.com");
    }

    private static Account account(AccountStatus status) {
        Account account = new Account();
        account.setUserId(USER_ID);
        account.setEmail("user@test.com");
        account.setAccountStatus(status);
        return account;
    }

    private static CreateAccountEvent event() {
        return CreateAccountEvent.builder()
                .email("user@test.com")
                .accountType(AccountType.SAVING)
                .currency(Currency.MX)
                .build();
    }
}
