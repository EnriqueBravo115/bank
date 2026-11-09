package dev.enrique.bank.service.impl;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.LimitType;
import dev.enrique.bank.commons.enums.MovementType;
import dev.enrique.bank.commons.enums.TimePeriod;
import dev.enrique.bank.commons.exception.TransactionLimitExceededException;
import dev.enrique.bank.dao.AccountRepository;
import dev.enrique.bank.dao.MovementRepository;
import dev.enrique.bank.dao.MovementUsage;
import dev.enrique.bank.dao.TransactionLimitRepository;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.model.TransactionLimit;

@ExtendWith(MockitoExtension.class)
class TransactionLimitServiceImplTest {
    // Thursday 2026-11-12, 15:30
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-12T15:30:00Z"), ZoneOffset.UTC);
    private static final String KEY = "txn-1";

    @Mock
    private TransactionLimitRepository transactionLimitRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private MovementRepository movementRepository;

    private TransactionLimitServiceImpl service;
    private final Account account = Account.builder().id(1L).accountNumber("ACC-1").currency(Currency.MX).build();

    @BeforeEach
    void setUp() {
        service = new TransactionLimitServiceImpl(transactionLimitRepository, accountRepository, movementRepository,
                CLOCK);
    }

    @ParameterizedTest
    @CsvSource({ "DAILY, 2026-11-12T00:00", "WEEKLY, 2026-11-09T00:00", "MONTHLY, 2026-11-01T00:00" })
    void countsTheMovementsSinceTheStartOfTheCalendarPeriod(TimePeriod period, LocalDateTime start) {
        limit(period, "100.00", 3);
        when(movementRepository.usageSince("ACC-1", MovementType.TRANSFER, start, KEY))
                .thenReturn(new MovementUsage(2, new BigDecimal("70.00")));

        assertThatCode(() -> service.check(account, LimitType.TRANSFER, new BigDecimal("30.00"), KEY))
                .doesNotThrowAnyException();
        verify(movementRepository).usageSince("ACC-1", MovementType.TRANSFER, start, KEY);
    }

    @ParameterizedTest
    @CsvSource({ "30.01, 2", "10.00, 3" })
    void rejectsTheMovementThatGoesOverTheAmountOrTheCount(BigDecimal amount, long count) {
        limit(TimePeriod.DAILY, "100.00", 3);
        when(movementRepository.usageSince(any(), any(), any(), any()))
                .thenReturn(new MovementUsage(count, new BigDecimal("70.00")));

        assertThatThrownBy(() -> service.check(account, LimitType.TRANSFER, amount, KEY))
                .isInstanceOf(TransactionLimitExceededException.class);
    }

    private void limit(TimePeriod period, String maxAmount, int maxTransactions) {
        when(transactionLimitRepository.findAllByAccountIdAndLimitType(1L, LimitType.TRANSFER))
                .thenReturn(List.of(TransactionLimit.builder().account(account).limitType(LimitType.TRANSFER)
                        .timePeriod(period).maxAmount(new BigDecimal(maxAmount)).maxTransactions(maxTransactions)
                        .currency(Currency.MX).build()));
    }
}
