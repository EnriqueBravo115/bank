package dev.enrique.bank.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.enrique.bank.service.impl.TransferServiceImpl;
import dev.enrique.bank.service.util.TransferHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.enrique.bank.commons.enums.Status;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.commons.exception.AccountNotFoundException;
import dev.enrique.bank.dao.AccountRepository;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.dto.request.TransferRequest;
import dev.enrique.bank.dto.response.TransferResponse;
import dev.enrique.bank.mapper.BasicMapper;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.model.Transaction;

@ExtendWith(MockitoExtension.class)
public class TransferServiceTest {
    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferHelper transferHelper;

    @Mock
    private BasicMapper basicMapper;

    @InjectMocks
    private TransferServiceImpl transferService;

    private Transaction transaction1;
    private Transaction transaction2;
    private TransferResponse transferRequest1;
    private TransferResponse transferRequest2;
    private Account sourceAccount;
    private Account targetAccount;

    @BeforeEach
    void setUp() {
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

        LocalDateTime now = LocalDateTime.now();

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
    void getTransferHistory_ShouldReturnGroupedTransactions() {
        Long accountId = Long.valueOf(1L);
        List<Transaction> transactions = Arrays.asList(transaction1, transaction2);

        when(transactionRepository.findAllByAccountId(accountId)).thenReturn(transactions);
        when(basicMapper.convertToResponse(transaction1, TransferResponse.class)).thenReturn(transferRequest1);
        when(basicMapper.convertToResponse(transaction2, TransferResponse.class)).thenReturn(transferRequest2);

        Map<String, Map<String, List<TransferResponse>>> result = transferService.getTransferHistory(accountId);
        assertNotNull(result);
        assertEquals(2, result.size());

        // Verify first transaction
        String dateKey1 = transaction1.getTransactionDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String amountKey1 = transaction1.getAmount().toString();

        assertTrue(result.containsKey(dateKey1));
        assertTrue(result.get(dateKey1).containsKey(amountKey1));
        assertEquals(1, result.get(dateKey1).get(amountKey1).size());
        assertEquals(transferRequest1, result.get(dateKey1).get(amountKey1).get(0));

        // Verify second transaction
        String dateKey2 = transaction2.getTransactionDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String amountKey2 = transaction2.getAmount().toString();

        assertTrue(result.containsKey(dateKey2));
        assertTrue(result.get(dateKey2).containsKey(amountKey2));
        assertEquals(1, result.get(dateKey2).get(amountKey2).size());
        assertEquals(transferRequest2, result.get(dateKey2).get(amountKey2).get(0));

        // Verify field mapping
        TransferResponse firstRequest = result.get(dateKey1).get(amountKey1).get(0);
        assertEquals("123456789", firstRequest.getSourceAccountNumber());
        assertEquals("987654321", firstRequest.getTargetAccountNumber());
        assertEquals(TransactionType.TRANSFER, firstRequest.getTransactionType());
    }

    @Test
    void reverseTransfer_shouldSuccessfullyReverseTransaction() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction1));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        transferService.reverseTransfer(1L);

        verify(transactionRepository).findById(1L);
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository, times(2)).save(any(Transaction.class));

        assertEquals(TransactionStatus.REVERSED, transaction1.getTransactionStatus());
        assertEquals(new BigDecimal(1200), sourceAccount.getBalance());
        assertEquals(new BigDecimal(300), targetAccount.getBalance());
    }

    @Test
    void reverseTransfer_whenTransactionAlreadyReversed_shouldThrowException() {
        transaction1.setTransactionStatus(TransactionStatus.REVERSED);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction1));

        assertThrows(IllegalStateException.class, () -> {
            transferService.reverseTransfer(1L);
        }, "Transaction is already reversed");
    }

    @Test
    void reverseTransfer_whenTransactionNotCompleted_shouldThrowException() {
        transaction1.setTransactionStatus(TransactionStatus.PENDING);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction1));

        assertThrows(IllegalStateException.class, () -> {
            transferService.reverseTransfer(1L);
        }, "Only completed can be reversed");
    }

    @Test
    void reverseTransfer_whenAccountsNotOpen_shouldThrowException() {
        sourceAccount.setStatus(Status.CLOSE);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction1));

        assertThrows(IllegalStateException.class, () -> {
            transferService.reverseTransfer(1L);
        }, "Both accounts must be open to reverse the transaction");
    }

    @Test
    void reverseTransfer_whenTargetAccountHasInsufficientBalance_shouldThrowException() {
        targetAccount.setBalance(new BigDecimal(100));
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction1));

        assertThrows(IllegalStateException.class, () -> {
            transferService.reverseTransfer(1L);
        }, "Insufficient balance in target account to reverse transaction");
    }

    @Test
    void reverseTransfer_shouldCreateReversalTransactionWithCorrectProperties() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction1));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        transferService.reverseTransfer(1L);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(transactionCaptor.capture());

        Transaction reversal = transactionCaptor.getAllValues().get(1);
        assertEquals(targetAccount, reversal.getSourceAccount());
        assertEquals(sourceAccount, reversal.getTargetAccount());
        assertEquals(transaction1.getAmount(), reversal.getAmount());
        assertEquals(TransactionType.REVERSAL, reversal.getTransactionType());
        assertEquals(TransactionStatus.PENDING, reversal.getTransactionStatus());
        assertEquals(transaction1, reversal.getOriginalTransaction());
    }

    @Test
    void hasSufficientFunds_shouldReturnTrueWhenEnoughBalance() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));

        assertTrue(transferService.hasSufficientFunds(1L, new BigDecimal(500)));
    }

    @Test
    void hasSufficientFunds_shouldReturnFalseWhenInsufficientBalance() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));

        assertFalse(transferService.hasSufficientFunds(1L, new BigDecimal(2000)));
    }

    @Test
    void hasSufficientFunds_shouldThrowWhenAccountNotFound() {
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> {
            transferService.hasSufficientFunds(1L, new BigDecimal(100));
        });
    }

    @Test
    void hasSufficientFunds_shouldThrowWhenAccountNotOpen() {
        sourceAccount.setStatus(Status.CLOSE);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));

        assertThrows(IllegalStateException.class, () -> {
            transferService.hasSufficientFunds(1L, new BigDecimal(100));
        });
    }

    @Test
    void hasSufficientFunds_shouldThrowWhenAmountIsNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            transferService.hasSufficientFunds(1L, null);
        });
    }

    @Test
    void hasSufficientFunds_shouldThrowWhenAmountIsZeroOrNegative() {
        assertThrows(IllegalArgumentException.class, () -> {
            transferService.hasSufficientFunds(1L, BigDecimal.ZERO);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            transferService.hasSufficientFunds(1L, new BigDecimal(-100));
        });
    }

    @Test
    void hasSufficientFunds_shouldThrowWhenAccountIdIsNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            transferService.hasSufficientFunds(null, new BigDecimal("100.00"));
        });
    }
}
