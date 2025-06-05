package dev.enrique.bank.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hibernate.mapping.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import dev.enrique.bank.TransactionServiceTestHelper;
import dev.enrique.bank.commons.constants.TestConstants;
import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.Status;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.dao.projection.TransactionBasicProjection;
import dev.enrique.bank.dao.projection.TransactionCommonProjection;
import dev.enrique.bank.dao.projection.TransactionDetailedProjection;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.model.Transaction;
import dev.enrique.bank.service.impl.TransactionServiceImpl;
import dev.enrique.bank.service.util.AccountHelper;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceImplTest {
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountHelper accountHelper;
    @InjectMocks
    private TransactionServiceImpl transactionService;

    private static final PageRequest pageable = PageRequest.of(0, 20);

    @Test
    void getTransactionHistory_ValidAccountId_ReturnsProjections() {
        List<TransactionDetailedProjection> expected = TransactionServiceTestHelper
                .createTransactionDetailedProjection();

        when(transactionRepository.findCompletedByAccountId(TestConstants.ACCOUNT_ID,
                TransactionDetailedProjection.class))
                .thenReturn(expected);

        List<TransactionDetailedProjection> result = transactionService.getTransactionHistory(TestConstants.ACCOUNT_ID);
        assertEquals(expected, result);
    }

    @Test
    void getAllTransactions_ValidAccountId_ReturnsPagedProjections() {
        Page<TransactionCommonProjection> expected = TransactionServiceTestHelper
                .createTransactionPageCommonProjections();

        when(transactionRepository.findCompletedByAccountId(TestConstants.ACCOUNT_ID, pageable))
                .thenReturn(expected);

        Page<TransactionCommonProjection> result = transactionService
                .getAllTransactions(TestConstants.ACCOUNT_ID, pageable);
        assertEquals(expected, result);
    }

    @Test
    void getTransactionByYearAndAccount_ValidInput_ReturnsProjections() {
        List<TransactionCommonProjection> expected = TransactionServiceTestHelper.createTransactionCommonProjections();

        when(transactionRepository.findByAccountIdAndYear(TestConstants.ACCOUNT_ID, TestConstants.YEAR))
                .thenReturn(expected);

        List<TransactionCommonProjection> result = transactionService
                .getTransactionByYearAndAccount(TestConstants.ACCOUNT_ID, TestConstants.YEAR);
        assertEquals(expected, result);
    }

    @Test
    void getAllTransactionsReversals_ValidAccountId_ReturnsBasicProjections() {
        List<TransactionBasicProjection> expected = TransactionServiceTestHelper.createTransactionBasicProjections();

        when(transactionRepository.findReversalsByAccountId(TestConstants.ACCOUNT_ID))
                .thenReturn(expected);

        List<TransactionBasicProjection> result = transactionService
                .getAllTransactionsReversals(TestConstants.ACCOUNT_ID);
        assertEquals(expected, result);
    }

    @Test
    void getAllTransactionsFromAccounts_ValidAccountIds_ReturnsCommonProjections() {
        List<TransactionCommonProjection> expected = TransactionServiceTestHelper.createTransactionCommonProjections();

        when(transactionRepository.findCompletedByAccountIdsIn(TestConstants.ACCOUNT_IDS))
                .thenReturn(expected);

        List<TransactionCommonProjection> result = transactionService
                .getAllTransactionsFromAccounts(TestConstants.ACCOUNT_IDS);
        assertEquals(expected, result);
    }

    // Aqui hay pdo ya que no se si este bien eso de que devuelva lo mismo sin
    // importar el id
    @Test
    void groupTransactionsByType_ValidAccountId_ReturnsGroupedMap() {
        List<TransactionCommonProjection> transactions = TransactionServiceTestHelper.createGroupedTransactions();

        when(transactionRepository.findCompletedByAccountId(TestConstants.ACCOUNT_ID,
                TransactionCommonProjection.class))
                .thenReturn(transactions);

        Map<TransactionType, List<TransactionCommonProjection>> result = transactionService
                .groupTransactionsByType(TestConstants.ACCOUNT_ID);

        assertEquals(2, result.size());
        assertEquals(2, result.get(TransactionType.TRANSFER).size());
        assertEquals(1, result.get(TransactionType.WITHDRAW).size());
    }

    @Test
    void sumTransactionsByType_ValidAccountId_ReturnsSumMap() {
        Long accountId = 1L;
        Transaction t1 = new Transaction();
        t1.setTransactionType(TransactionType.TRANSFER);
        t1.setAmount(new BigDecimal("100.00"));

        Transaction t2 = new Transaction();
        t2.setTransactionType(TransactionType.TRANSFER);
        t2.setAmount(new BigDecimal("200.00"));

        Transaction t3 = new Transaction();
        t3.setTransactionType(TransactionType.WITHDRAW);
        t3.setAmount(new BigDecimal("50.00"));

        List<Transaction> transactions = Arrays.asList(t1, t2, t3);

        when(transactionRepository.findAllByAccountId(accountId))
                .thenReturn(transactions);

        Map<TransactionType, BigDecimal> result = transactionService.sumTransactionsByType(accountId);

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("300.00"), result.get(TransactionType.TRANSFER));
        assertEquals(new BigDecimal("50.00"), result.get(TransactionType.WITHDRAW));
        verify(accountHelper).validateAccountId(accountId);
    }

    @Test
    void getTransactionYearStatistics_ValidAccountId_ReturnsStatistics() {
        Long accountId = 1L;
        Transaction t1 = new Transaction();
        t1.setTransactionDate(LocalDateTime.of(2020, 1, 1, 0, 0));

        Transaction t2 = new Transaction();
        t2.setTransactionDate(LocalDateTime.of(2021, 1, 1, 0, 0));

        Transaction t3 = new Transaction();
        t3.setTransactionDate(LocalDateTime.of(2021, 1, 1, 0, 0));

        List<Transaction> transactions = Arrays.asList(t1, t2, t3);

        when(transactionRepository.findAllByAccountId(accountId))
                .thenReturn(transactions);

        IntSummaryStatistics result = transactionService.getTransactionYearStatistics(accountId);

        assertEquals(3, result.getCount());
        assertEquals(2020, result.getMin());
        assertEquals(2021, result.getMax());
        verify(accountHelper).validateAccountId(accountId);
    }

    @Test
    void partitionTransactionsByAmount_ValidInput_ReturnsPartitionedMap() {
        Long accountId = 1L;
        BigDecimal threshold = new BigDecimal("100.00");

        TransactionBasicProjection t1 = mock(TransactionBasicProjection.class);
        when(t1.getAmount()).thenReturn(new BigDecimal("150.00"));

        TransactionBasicProjection t2 = mock(TransactionBasicProjection.class);
        when(t2.getAmount()).thenReturn(new BigDecimal("50.00"));

        List<TransactionBasicProjection> transactions = Arrays.asList(t1, t2);

        when(transactionRepository.findCompletedByAccountId(eq(accountId), eq(TransactionBasicProjection.class)))
                .thenReturn(transactions);

        Map<Boolean, List<TransactionBasicProjection>> result = transactionService
                .partitionTransactionsByAmount(accountId, threshold);

        assertEquals(2, result.size());
        assertEquals(1, result.get(true).size());
        assertEquals(1, result.get(false).size());
        verify(accountHelper).validateAccountId(accountId);
    }

    @Test
    void getTransactionTypeSummary_ValidAccountId_ReturnsFormattedSummary() {
        Long accountId = 1L;
        Transaction t1 = new Transaction();
        t1.setTransactionType(TransactionType.TRANSFER);
        t1.setAmount(new BigDecimal("100.00"));

        Transaction t2 = new Transaction();
        t2.setTransactionType(TransactionType.TRANSFER);
        t2.setAmount(new BigDecimal("200.00"));

        List<Transaction> transactions = Arrays.asList(t1, t2);

        when(transactionRepository.findAllByAccountId(accountId))
                .thenReturn(transactions);

        Map<TransactionType, String> result = transactionService.getTransactionTypeSummary(accountId);

        assertEquals(1, result.size());
        assertTrue(result.get(TransactionType.TRANSFER).contains("Count: 2, Total: $300.00"));
        verify(accountHelper).validateAccountId(accountId);
    }

    @Test
    void calculateTotalTransactionAmount_ValidAccountId_ReturnsSum() {
        Long accountId = 1L;
        Transaction t1 = new Transaction();
        t1.setAmount(new BigDecimal("100.00"));

        Transaction t2 = new Transaction();
        t2.setAmount(new BigDecimal("200.00"));

        List<Transaction> transactions = Arrays.asList(t1, t2);

        when(transactionRepository.findAllByAccountId(accountId))
                .thenReturn(transactions);

        BigDecimal result = transactionService.calculateTotalTransactionAmount(accountId);

        assertEquals(new BigDecimal("300.00"), result);
        verify(accountHelper).validateAccountId(accountId);
    }
    @Test
    void calculateTotalAmountByType_ValidInput_ReturnsSum() {
        Long accountId = 1L;
        TransactionType type = TransactionType.TRANSFER;

        Transaction t1 = new Transaction();
        t1.setTransactionType(TransactionType.TRANSFER);
        t1.setAmount(new BigDecimal("100.00"));

        Transaction t2 = new Transaction();
        t2.setTransactionType(TransactionType.WITHDRAW);
        t2.setAmount(new BigDecimal("50.00"));

        Transaction t3 = new Transaction();
        t3.setTransactionType(TransactionType.TRANSFER);
        t3.setAmount(new BigDecimal("200.00"));

        List<Transaction> transactions = Arrays.asList(t1, t2, t3);

        when(transactionRepository.findAllByAccountId(accountId))
                .thenReturn(transactions);

        BigDecimal result = transactionService.calculateTotalAmountByType(accountId, type);

        assertEquals(new BigDecimal("300.00"), result);
        verify(accountHelper).validateAccountIdAndTransactionType(accountId, type);
    }

    // @Test
    // void calculateTransferFee_AmountZeroOrLess_ThrowsException() {
    // BigDecimal amount = BigDecimal.ZERO;
    // Currency currency = new Currency();

    // assertThrows(IllegalArgumentException.class, () ->
    // transactionService.calculateTransferFee(amount, currency));
    // }

    // @Test
    // void calculateTransferFee_ValidInput_ReturnsFee() {
    // BigDecimal amount = new BigDecimal("1000.00");
    // Currency currency = new Currency();
    // currency.setFeePercentage(new BigDecimal("0.01"));
    // currency.setMinimumFee(new BigDecimal("5.00"));
    // currency.setMaximumFee(new BigDecimal("20.00"));

    // BigDecimal result = transactionService.calculateTransferFee(amount,
    // currency);

    // assertEquals(new BigDecimal("10.00"), result);
    // }

    // @Test
    // void calculateTransferFee_FeeBelowMinimum_ReturnsMinimum() {
    // BigDecimal amount = new BigDecimal("100.00");
    // Currency currency = new Currency();
    // currency.setFeePercentage(new BigDecimal("0.01"));
    // currency.setMinimumFee(new BigDecimal("5.00"));
    // currency.setMaximumFee(new BigDecimal("20.00"));

    // BigDecimal result = transactionService.calculateTransferFee(amount,
    // currency);

    // assertEquals(new BigDecimal("5.00"), result);
    // }

    // @Test
    // void calculateTransferFee_FeeAboveMaximum_ReturnsMaximum() {
    // BigDecimal amount = new BigDecimal("3000.00");
    // Currency currency = new Currency();
    // currency.setFeePercentage(new BigDecimal("0.01"));
    // currency.setMinimumFee(new BigDecimal("5.00"));
    // currency.setMaximumFee(new BigDecimal("20.00"));

    // BigDecimal result = transactionService.calculateTransferFee(amount,
    // currency);

    // assertEquals(new BigDecimal("20.00"), result);
    // }

    @Test
    void hasSufficientFunds_ValidInput_ReturnsTrueWhenSufficient() {
        Long accountId = 1L;
        BigDecimal amount = new BigDecimal("100.00");
        Account account = new Account();
        account.setBalance(new BigDecimal("200.00"));

        when(accountHelper.getAccountById(accountId))
                .thenReturn(account);

        boolean result = transactionService.hasSufficientFunds(accountId, amount);

        assertTrue(result);
    }

    @Test
    void hasSufficientFunds_ValidInput_ReturnsFalseWhenInsufficient() {
        Long accountId = 1L;
        BigDecimal amount = new BigDecimal("300.00");
        Account account = new Account();
        account.setBalance(new BigDecimal("200.00"));

        when(accountHelper.getAccountById(accountId))
                .thenReturn(account);

        boolean result = transactionService.hasSufficientFunds(accountId, amount);

        assertFalse(result);
    }

    @Test
    void getTransferLimit_AccountNotOpen_ThrowsException() {
        Long accountId = 1L;
        Account account = new Account();
        account.setStatus(Status.CLOSE);
        account.setBalance(new BigDecimal("1000.00"));

        when(accountHelper.getAccountById(accountId))
                .thenReturn(account);

        assertThrows(IllegalStateException.class, () -> transactionService.getTransferLimit(accountId));
    }

    @Test
    void getTransferLimit_AccountOpen_ReturnsDoubleBalance() {
        Long accountId = 1L;
        BigDecimal balance = new BigDecimal("1000.00");
        Account account = new Account();
        account.setStatus(Status.OPEN);
        account.setBalance(balance);

        when(accountHelper.getAccountById(accountId))
                .thenReturn(account);

        BigDecimal result = transactionService.getTransferLimit(accountId);

        assertEquals(balance.multiply(new BigDecimal("2")), result);
    }

    // @Test
    // void getAllUniqueTransactionDescriptions_ValidAccountId_ReturnsUniqueWords()
    // {
    // Long accountId = 1L;
    // Transaction t1 = new Transaction();
    // t1.setDescription("Salary payment");

    // Transaction t2 = new Transaction();
    // t2.setDescription("Grocery shopping");

    // Transaction t3 = new Transaction();
    // t3.setDescription("Salary bonus");

    // List<Transaction> transactions = Arrays.asList(t1, t2, t3);

    // when(transactionRepository.findAllByAccountId(accountId))
    // .thenReturn(transactions);

    // Set<String> result =
    // transactionService.getAllUniqueTransactionDescriptions(accountId);

    // assertEquals(4, result.size());
    // assertTrue(result.contains("Salary"));
    // assertTrue(result.contains("payment"));
    // assertTrue(result.contains("Grocery"));
    // assertTrue(result.contains("shopping"));
    // assertTrue(result.contains("bonus"));
    // verify(accountHelper).validateAccountId(accountId);
    // }

    @Test
    void getAllTransactionDescriptions_ValidAccountId_ReturnsConcatenatedString() {
        Long accountId = 1L;
        Transaction t1 = new Transaction();
        t1.setDescription("Salary");

        Transaction t2 = new Transaction();
        t2.setDescription("Bonus");

        List<Transaction> transactions = Arrays.asList(t1, t2);

        when(transactionRepository.findAllByAccountId(accountId))
                .thenReturn(transactions);

        String result = transactionService.getAllTransactionDescriptions(accountId);

        assertEquals("Salary, Bonus", result);
        verify(accountHelper).validateAccountId(accountId);
    }

    @Test
    void getFormattedAverageBalance_ValidAccountIds_ReturnsFormattedString() {
        List<Long> accountIds = Arrays.asList(1L, 2L);
        Account a1 = new Account();
        a1.setBalance(new BigDecimal("1000.00"));

        Account a2 = new Account();
        a2.setBalance(new BigDecimal("2000.00"));

        when(accountHelper.getAccountById(1L)).thenReturn(a1);
        when(accountHelper.getAccountById(2L)).thenReturn(a2);

        String result = transactionService.getFormattedAverageBalance(accountIds);

        assertEquals("$1,500.00", result);
        accountIds.forEach(id -> verify(accountHelper).validateAccountId(id));
    }

    @Test
    void findMaxTransaction_ValidAccountId_ReturnsMaxTransaction() {
        Long accountId = 1L;
        TransactionBasicProjection t1 = mock(TransactionBasicProjection.class);
        when(t1.getAmount()).thenReturn(new BigDecimal("100.00"));

        TransactionBasicProjection t2 = mock(TransactionBasicProjection.class);
        when(t2.getAmount()).thenReturn(new BigDecimal("200.00"));

        List<TransactionBasicProjection> transactions = Arrays.asList(t1, t2);

        when(transactionRepository.findCompletedByAccountId(eq(accountId), eq(TransactionBasicProjection.class)))
                .thenReturn(transactions);

        Optional<TransactionBasicProjection> result = transactionService.findMaxTransaction(accountId);

        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("200.00"), result.get().getAmount());
        verify(accountHelper).validateAccountId(accountId);
    }

    @Test
    void getAverageDaysBetweenTransactions_LessThanTwoTransactions_ReturnsZero() {
        Long accountId = 1L;
        Transaction t1 = new Transaction();
        t1.setTransactionDate(LocalDateTime.now());

        List<Transaction> transactions = Collections.singletonList(t1);

        when(transactionRepository.findAllByAccountId(accountId))
                .thenReturn(transactions);

        double result = transactionService.getAverageDaysBetweenTransactions(accountId);

        assertEquals(0, result);
        verify(accountHelper).validateAccountId(accountId);
    }

    @Test
    void getAverageDaysBetweenTransactions_MultipleTransactions_ReturnsAverage() {
        Long accountId = 1L;
        Transaction t1 = new Transaction();
        t1.setTransactionDate(LocalDateTime.of(2023, 1, 1, 0, 0));

        Transaction t2 = new Transaction();
        t2.setTransactionDate(LocalDateTime.of(2023, 1, 3, 0, 0));

        Transaction t3 = new Transaction();
        t3.setTransactionDate(LocalDateTime.of(2023, 1, 6, 0, 0));

        List<Transaction> transactions = Arrays.asList(t1, t2, t3);

        when(transactionRepository.findAllByAccountId(accountId))
                .thenReturn(transactions);

        double result = transactionService.getAverageDaysBetweenTransactions(accountId);

        assertEquals(2.5, result);
        verify(accountHelper).validateAccountId(accountId);
    }
}
