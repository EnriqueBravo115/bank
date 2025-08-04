package dev.enrique.bank.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import dev.enrique.bank.TransactionServiceTestHelper;
import dev.enrique.bank.constants.TestConstants;
import dev.enrique.bank.enums.Currency;
import dev.enrique.bank.enums.AccountStatus;
import dev.enrique.bank.enums.TransactionType;
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

    private final PageRequest pageable = PageRequest.of(0, 20);
    private Transaction t1;
    private Transaction t2;
    private Transaction t3;
    private Account a1;
    private Account a2;

    @BeforeEach
    void setUp() {
        t1 = Transaction.builder()
                .transactionType(TransactionType.TRANSFER)
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDateTime.of(2020, 1, 1, 0, 0))
                .description("Salary")
                .build();

        t2 = Transaction.builder()
                .transactionType(TransactionType.TRANSFER)
                .amount(new BigDecimal("200.00"))
                .transactionDate(LocalDateTime.of(2021, 1, 1, 0, 0))
                .description("Bonus")
                .build();

        t3 = Transaction.builder()
                .transactionType(TransactionType.WITHDRAW)
                .amount(new BigDecimal("50.00"))
                .transactionDate(LocalDateTime.of(2021, 1, 1, 0, 0))
                .build();

        a1 = Account.builder()
                .balance(new BigDecimal("1000"))
                .status(AccountStatus.OPEN)
                .build();

        a2 = Account.builder()
                .balance(new BigDecimal("2000"))
                .status(AccountStatus.OPEN)
                .build();
    }

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

    @Test
    void groupTransactionsByType_ValidAccountId_ReturnsGroupedMap() {
        when(transactionRepository.findCompletedByAccountId(TestConstants.ACCOUNT_ID,
                TransactionCommonProjection.class))
                .thenReturn(TransactionServiceTestHelper.createGroupedTransactions());

        Map<TransactionType, List<TransactionCommonProjection>> result = transactionService
                .groupTransactionsByType(TestConstants.ACCOUNT_ID);

        assertEquals(2, result.size());
        assertEquals(2, result.get(TransactionType.TRANSFER).size());
        assertEquals(1, result.get(TransactionType.WITHDRAW).size());
    }

    @Test
    void sumTransactionsByType_ValidAccountId_ReturnsSumMap() {
        List<Transaction> transactions = Arrays.asList(t1, t2, t3);

        when(transactionRepository.findAllByAccountId(TestConstants.ACCOUNT_ID))
                .thenReturn(transactions);

        Map<TransactionType, BigDecimal> result = transactionService.sumTransactionsByType(TestConstants.ACCOUNT_ID);

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("300.00"), result.get(TransactionType.TRANSFER));
        assertEquals(new BigDecimal("50.00"), result.get(TransactionType.WITHDRAW));
    }

    @Test
    void getTransactionYearStatistics_ValidAccountId_ReturnsStatistics() {
        List<Transaction> transactions = Arrays.asList(t1, t2, t3);

        when(transactionRepository.findAllByAccountId(TestConstants.ACCOUNT_ID))
                .thenReturn(transactions);

        IntSummaryStatistics result = transactionService.getTransactionYearStatistics(TestConstants.ACCOUNT_ID);

        assertEquals(3, result.getCount());
        assertEquals(2020, result.getMin());
        assertEquals(2021, result.getMax());
    }

    @Test
    void partitionTransactionsByAmount_ValidInput_ReturnsPartitionedMap() {
        BigDecimal threshold = new BigDecimal("60.00");

        when(transactionRepository.findCompletedByAccountId(TestConstants.ACCOUNT_ID, TransactionBasicProjection.class))
                .thenReturn(TransactionServiceTestHelper.createTransactionBasicProjections());

        Map<Boolean, List<TransactionBasicProjection>> result = transactionService
                .partitionTransactionsByAmount(TestConstants.ACCOUNT_ID, threshold);

        assertEquals(2, result.size());
        assertEquals(1, result.get(true).size());
        assertEquals(1, result.get(false).size());
    }

    @Test
    void getTransactionTypeSummary_ValidAccountId_ReturnsFormattedSummary() {
        List<Transaction> transactions = Arrays.asList(t1, t2);

        when(transactionRepository.findAllByAccountId(TestConstants.ACCOUNT_ID))
                .thenReturn(transactions);

        Map<TransactionType, String> result = transactionService.getTransactionTypeSummary(TestConstants.ACCOUNT_ID);

        assertEquals(1, result.size());
        assertTrue(result.get(TransactionType.TRANSFER).contains("Count: 2, Total: $300.00"));
    }

    @Test
    void calculateTotalTransactionAmount_ValidAccountId_ReturnsSum() {
        List<Transaction> transactions = Arrays.asList(t1, t2);

        when(transactionRepository.findAllByAccountId(TestConstants.ACCOUNT_ID))
                .thenReturn(transactions);

        BigDecimal result = transactionService.calculateTotalTransactionAmount(TestConstants.ACCOUNT_ID);
        assertEquals(new BigDecimal("300.00"), result);
    }

    @Test
    void calculateTotalAmountByType_ValidInput_ReturnsSum() {
        List<Transaction> transactions = Arrays.asList(t1, t2, t3);

        when(transactionRepository.findAllByAccountId(TestConstants.ACCOUNT_ID))
                .thenReturn(transactions);

        BigDecimal result = transactionService.calculateTotalAmountByType(TestConstants.ACCOUNT_ID,
                TransactionType.TRANSFER);
        assertEquals(new BigDecimal("300.00"), result);
    }

    @Test
    void calculateTransferFee_AmountZeroOrLess_ThrowsException() {
        BigDecimal amount = BigDecimal.ZERO;
        assertThrows(IllegalArgumentException.class,
                () -> transactionService.calculateTransferFee(amount, Currency.MXN));
    }

    @Test
    void calculateTransferFee_ValidInput_ReturnsFee() {
        BigDecimal result = transactionService.calculateTransferFee(new BigDecimal("1000.00"), Currency.MXN);
        assertEquals(new BigDecimal("30.00"), result);
    }

    @Test
    void hasSufficientFunds_ValidInput_ReturnsTrueWhenSufficient() {
        when(accountHelper.getAccountById(TestConstants.ACCOUNT_ID))
                .thenReturn(a1);

        boolean result = transactionService.hasSufficientFunds(TestConstants.ACCOUNT_ID, new BigDecimal("100"));
        assertTrue(result);
    }

    @Test
    void hasSufficientFunds_ValidInput_ReturnsFalseWhenInsufficient() {
        when(accountHelper.getAccountById(TestConstants.ACCOUNT_ID))
                .thenReturn(a1);

        boolean result = transactionService.hasSufficientFunds(TestConstants.ACCOUNT_ID, new BigDecimal("2000"));
        assertFalse(result);
    }

    @Test
    void getTransferLimit_AccountNotOpen_ThrowsException() {
        a1.setStatus(AccountStatus.CLOSE);

        when(accountHelper.getAccountById(TestConstants.ACCOUNT_ID))
                .thenReturn(a1);

        assertThrows(IllegalStateException.class, () -> transactionService.getTransferLimit(TestConstants.ACCOUNT_ID));
    }

    @Test
    void getTransferLimit_AccountOpen_ReturnsDoubleBalance() {
        when(accountHelper.getAccountById(TestConstants.ACCOUNT_ID))
                .thenReturn(a1);

        BigDecimal result = transactionService.getTransferLimit(TestConstants.ACCOUNT_ID);
        assertEquals(new BigDecimal("2000"), result);
    }

    @Test
    void getAllUniqueTransactionDescriptions_ValidAccountId_ReturnsUniqueWords() {
        t1.setDescription("Salary payment");
        t2.setDescription("Grocery shopping");
        t3.setDescription("Salary payment");

        List<Transaction> transactions = Arrays.asList(t1, t2, t3);

        when(transactionRepository.findAllByAccountId(TestConstants.ACCOUNT_ID))
                .thenReturn(transactions);

        Set<String> result = transactionService.getAllUniqueTransactionDescriptions(TestConstants.ACCOUNT_ID);

        assertEquals(4, result.size());
        assertTrue(result.contains("Salary"));
        assertTrue(result.contains("payment"));
        assertTrue(result.contains("Grocery"));
        assertTrue(result.contains("shopping"));
    }

    @Test
    void getAllUniqueTransactionDescriptions_shouldReturnUniqueWordsFromDescriptions() {
        t1.setDescription("Salary payment January");
        t2.setDescription("Salary payment February");
        t3.setDescription(null);

        when(transactionRepository.findAllByAccountId(TestConstants.ACCOUNT_ID))
                .thenReturn(List.of(t1, t2, t3));

        Set<String> result = transactionService.getAllUniqueTransactionDescriptions(TestConstants.ACCOUNT_ID);
        Set<String> expected = Set.of("Salary", "payment", "January", "February");
        assertEquals(expected, result);
    }

    @Test
    void getAllTransactionDescriptions_ValidAccountId_ReturnsConcatenatedString() {
        List<Transaction> transactions = Arrays.asList(t1, t2);

        when(transactionRepository.findAllByAccountId(TestConstants.ACCOUNT_ID))
                .thenReturn(transactions);

        String result = transactionService.getAllTransactionDescriptions(TestConstants.ACCOUNT_ID);
        assertEquals("Salary, Bonus", result);
    }

    @Test
    void getFormattedAverageBalance_ValidAccountIds_ReturnsFormattedString() {
        when(accountHelper.getAccountById(1L)).thenReturn(a1);
        when(accountHelper.getAccountById(2L)).thenReturn(a2);

        String result = transactionService.getFormattedAverageBalance(TestConstants.ACCOUNT_IDS);
        assertEquals("$1,500.00", result);
    }

    @Test
    void findMaxTransaction_ValidAccountId_ReturnsMaxTransaction() {
        when(transactionRepository.findCompletedByAccountId(TestConstants.ACCOUNT_ID, TransactionBasicProjection.class))
                .thenReturn(TransactionServiceTestHelper.createTransactionBasicProjections());

        Optional<TransactionBasicProjection> result = transactionService.findMaxTransaction(TestConstants.ACCOUNT_ID);

        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("100.00"), result.get().getAmount());
    }

    @Test
    void getAverageDaysBetweenTransactions_LessThanTwoTransactions_ReturnsZero() {
        when(transactionRepository.findAllByAccountId(TestConstants.ACCOUNT_ID))
                .thenReturn(Collections.singletonList(t1));

        double result = transactionService.getAverageDaysBetweenTransactions(TestConstants.ACCOUNT_ID);
        assertEquals(0, result);
    }

    @Test
    void getAverageDaysBetweenTransactions_MultipleTransactions_ReturnsAverage() {
        t1.setTransactionDate(LocalDateTime.of(2023, 1, 1, 0, 0));
        t2.setTransactionDate(LocalDateTime.of(2023, 1, 3, 0, 0));
        t3.setTransactionDate(LocalDateTime.of(2023, 1, 6, 0, 0));

        List<Transaction> transactions = Arrays.asList(t1, t2, t3);

        when(transactionRepository.findAllByAccountId(TestConstants.ACCOUNT_ID))
                .thenReturn(transactions);

        double result = transactionService.getAverageDaysBetweenTransactions(TestConstants.ACCOUNT_ID);
        assertEquals(2.5, result);
    }
}
