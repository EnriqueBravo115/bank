package dev.enrique.bank.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.enrique.bank.commons.dto.request.ClientServiceRequest;
import dev.enrique.bank.commons.dto.request.ClientTransferRequest;
import dev.enrique.bank.commons.enums.AccountStatus;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.exception.AccountNotFoundException;
import dev.enrique.bank.commons.exception.AccountNotOperableException;
import dev.enrique.bank.dao.AccountBalanceRepository;
import dev.enrique.bank.dao.AccountRepository;
import dev.enrique.bank.dao.MovementRepository;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.model.AccountBalance;
import dev.enrique.bank.model.Movement;
import dev.enrique.bank.service.FundsValidationService;

@ExtendWith(MockitoExtension.class)
class MovementServiceImplTest {
    private static final String SOURCE = "10000000000000000001";
    private static final String TARGET = "10000000000000000002";
    private static final BigDecimal AMOUNT = new BigDecimal("100.00");
    private static final String KEY = "txn-1";

    @Mock
    private FundsValidationService fundsValidationService;
    @Mock
    private AccountBalanceRepository accountBalanceRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private MovementRepository movementRepository;
    @InjectMocks
    private MovementServiceImpl movementService;

    @BeforeEach
    void fundsAndLimitsOk() {
        lenient().when(fundsValidationService.hasSufficientFunds(any(), any())).thenReturn(true);
        lenient().when(fundsValidationService.isWithinTransactionLimit(any(), any(), any())).thenReturn(true);
        lenient().when(fundsValidationService.isWithinDailyLimit(any(), any(), any())).thenReturn(true);
    }

    @Test
    void shouldTransferBetweenOpenAccounts() {
        Account source = account(SOURCE, AccountStatus.OPEN);
        Account target = account(TARGET, AccountStatus.OPEN);
        balance(source, "500.00");
        balance(target, "0.00");

        var result = movementService.processTransfer(KEY, new ClientTransferRequest(AMOUNT, SOURCE, TARGET));

        assertThat(result.transactionStatus()).isEqualTo(TransactionStatus.COMPLETED);
        verify(accountBalanceRepository, times(2)).save(any());
    }

    @Test
    void shouldReturnStoredResultForRepeatedKeyWithoutMovingMoney() {
        when(movementRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(Movement.builder()
                .idempotencyKey(KEY).status(TransactionStatus.COMPLETED).reason("Valid Transaction").build()));

        var result = movementService.processTransfer(KEY, new ClientTransferRequest(AMOUNT, SOURCE, TARGET));

        assertThat(result.transactionStatus()).isEqualTo(TransactionStatus.COMPLETED);
        verify(movementRepository, never()).saveAndFlush(any());
        verify(accountBalanceRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, names = "OPEN", mode = EnumSource.Mode.EXCLUDE)
    void shouldRejectTransferFromAccountThatIsNotOpen(AccountStatus status) {
        account(SOURCE, status);

        assertThatThrownBy(() -> movementService.processTransfer(KEY, new ClientTransferRequest(AMOUNT, SOURCE, TARGET)))
                .isInstanceOf(AccountNotOperableException.class)
                .hasMessage("Account " + SOURCE + " is " + status + " and cannot operate");

        verify(accountBalanceRepository, never()).save(any());
    }

    @Test
    void shouldRejectTransferToFrozenAccount() {
        account(SOURCE, AccountStatus.OPEN);
        account(TARGET, AccountStatus.FROZEN);

        assertThatThrownBy(() -> movementService.processTransfer(KEY, new ClientTransferRequest(AMOUNT, SOURCE, TARGET)))
                .isInstanceOf(AccountNotOperableException.class)
                .hasMessageContaining(TARGET);

        verify(accountBalanceRepository, never()).save(any());
    }

    @Test
    void shouldRejectDebitFromFrozenAccount() {
        account(SOURCE, AccountStatus.FROZEN);

        assertThatThrownBy(() -> movementService.processService(KEY, new ClientServiceRequest(SOURCE, AMOUNT)))
                .isInstanceOf(AccountNotOperableException.class);

        verify(accountBalanceRepository, never()).save(any());
    }

    @Test
    void shouldRejectUnknownAccount() {
        when(accountRepository.findEntityByAccountNumber(SOURCE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movementService.processService(KEY, new ClientServiceRequest(SOURCE, AMOUNT)))
                .isInstanceOf(AccountNotFoundException.class);
    }

    private Account account(String number, AccountStatus status) {
        Account account = Account.builder().accountNumber(number).accountStatus(status).build();
        lenient().when(accountRepository.findEntityByAccountNumber(number)).thenReturn(Optional.of(account));
        return account;
    }

    private void balance(Account account, String amount) {
        AccountBalance balance = AccountBalance.builder().account(account).balance(new BigDecimal(amount)).build();
        when(accountBalanceRepository.findLatestByAccountNumber(account.getAccountNumber()))
                .thenReturn(Optional.of(balance));
    }
}
