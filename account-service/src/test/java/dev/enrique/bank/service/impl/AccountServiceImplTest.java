package dev.enrique.bank.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.enrique.bank.broker.event.CreateAccountEvent;
import dev.enrique.bank.commons.enums.AccountStatus;
import dev.enrique.bank.commons.enums.AccountType;
import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.util.ClabeGenerator;
import dev.enrique.bank.dao.AccountRepository;
import dev.enrique.bank.model.Account;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {
    private static final String USER_ID = "keycloak-user-1";

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private ClabeGenerator clabeGenerator;
    @InjectMocks
    private AccountServiceImpl accountService;

    @Test
    void shouldCreateAccountForNewUser() {
        when(accountRepository.existsByUserId(USER_ID)).thenReturn(false);
        when(clabeGenerator.generate()).thenReturn("012345678901234567");
        when(accountRepository.existsByClabe("012345678901234567")).thenReturn(false);

        accountService.createAccount(event(), USER_ID);

        ArgumentCaptor<Account> saved = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getValue().getEmail()).isEqualTo("user@test.com");
        assertThat(saved.getValue().getAccountType()).isEqualTo(AccountType.SAVING);
        assertThat(saved.getValue().getCurrency()).isEqualTo(Currency.MX);
        assertThat(saved.getValue().getAccountStatus()).isEqualTo(AccountStatus.OPEN);
    }

    @Test
    void shouldIgnoreRepeatedAccountRequest() {
        when(accountRepository.existsByUserId(USER_ID)).thenReturn(true);

        accountService.createAccount(event(), USER_ID);

        verify(accountRepository, never()).save(any());
    }

    private static CreateAccountEvent event() {
        return CreateAccountEvent.builder()
                .email("user@test.com")
                .accountType(AccountType.SAVING)
                .currency(Currency.MX)
                .build();
    }
}
