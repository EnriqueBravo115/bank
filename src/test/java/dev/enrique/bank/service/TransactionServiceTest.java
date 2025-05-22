package dev.enrique.bank.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.Status;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.dao.AccountRepository;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.dto.response.TransferResponse;
import dev.enrique.bank.mapper.BasicMapper;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.model.Transaction;
import dev.enrique.bank.service.impl.TransactionServiceImpl;
import dev.enrique.bank.service.util.TransferHelper;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private BasicMapper basicMapper;
    @Mock
    private TransferHelper transferHelper;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private Transaction transaction1;
    private Transaction transaction2;
    private TransferResponse transferRequest1;
    private TransferResponse transferRequest2;
    private Account sourceAccount;
    private Account targetAccount;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        sourceAccount = Account.builder()
                .id(Long.valueOf(1L))
                .accountNumber("123456789")
                .balance(BigDecimal.valueOf(1000))
                .status(Status.OPEN)
                .build();

        targetAccount = Account.builder()
                .id(Long.valueOf(2L))
                .accountNumber("987654321")
                .balance(BigDecimal.valueOf(500))
                .status(Status.OPEN)
                .build();

        transaction1 = Transaction.builder()
                .id(Long.valueOf(1L))
                .amount(BigDecimal.valueOf(200))
                .description("Transfer 1")
                .transactionDate(now)
                .transactionType(TransactionType.TRANSFER)
                .transactionStatus(TransactionStatus.COMPLETED)
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .build();

        transaction2 = Transaction.builder()
                .id(Long.valueOf(2L))
                .amount(BigDecimal.valueOf(1000))
                .description("Transfer 2")
                .transactionDate(now.plusSeconds(1))
                .transactionType(TransactionType.TRANSFER)
                .transactionStatus(TransactionStatus.COMPLETED)
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .build();

        transferRequest1 = TransferResponse.builder()
                .id(Long.valueOf(1L))
                .description("Transfer to relatives")
                .sourceAccountNumber("123456789")
                .targetAccountNumber("987654321")
                .transactionType(TransactionType.TRANSFER)
                .transactionStatus(TransactionStatus.PROCESSING)
                .build();

        transferRequest2 = TransferResponse.builder()
                .id(Long.valueOf(2L))
                .description("Transfer to relatives")
                .sourceAccountNumber("123456789")
                .targetAccountNumber("987654321")
                .transactionType(TransactionType.TRANSFER)
                .transactionStatus(TransactionStatus.PROCESSING)
                .build();
    }

    @Test
    void hasSufficientFunds_shouldReturnTrueWhenEnoughBalance() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));

        assertTrue(transactionService.hasSufficientFunds(1L, new BigDecimal(500)));
    }

    @Test
    void calculateTransferFee_ShouldThrowWhenAmountIsZeroOrNegative() {
        assertAll(
                () -> {
                    IllegalArgumentException exception = assertThrows(
                            IllegalArgumentException.class,
                            () -> transactionService.calculateTransferFee(BigDecimal.ZERO, Currency.USD));
                    assertEquals("Amount must be greater than zero", exception.getMessage());
                },
                () -> {
                    IllegalArgumentException exception = assertThrows(
                            IllegalArgumentException.class,
                            () -> transactionService.calculateTransferFee(new BigDecimal("-100"), Currency.USD));
                    assertEquals("Amount must be greater than zero", exception.getMessage());
                });
    }
}
