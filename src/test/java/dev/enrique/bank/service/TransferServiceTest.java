package dev.enrique.bank.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.ScheduledTransferStatus;
import dev.enrique.bank.commons.enums.Status;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.commons.exception.TransferException;
import dev.enrique.bank.config.ScheduledTransferJob;
import dev.enrique.bank.dao.AccountRepository;
import dev.enrique.bank.dao.ScheduledTransferRepository;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.dto.request.TransferRequest;
import dev.enrique.bank.dto.response.TransferResponse;
import dev.enrique.bank.mapper.BasicMapper;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.model.ScheduledTransfer;
import dev.enrique.bank.model.Transaction;
import dev.enrique.bank.service.impl.TransferServiceImpl;

@ExtendWith(MockitoExtension.class)
public class TransferServiceTest {
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private ScheduledTransferRepository scheduledTransferRepository;
    @Mock
    private ScheduledExecutorService scheduledExecutorService;
    @Mock
    private BasicMapper basicMapper;
    @Mock
    private Scheduler quartzScheduler;

    @InjectMocks
    private TransferServiceImpl transferService;

    private Transaction transaction1;
    private Transaction transaction2;
    private TransferResponse transferRequest1;
    private TransferResponse transferRequest2;
    private Account sourceAccount;
    private Account targetAccount;
    private TransferRequest validRequest;
    private LocalDateTime futureDate;

    @BeforeEach
    void setUp() {
        futureDate = LocalDateTime.now().plusDays(1);
        LocalDateTime now = LocalDateTime.now();

        validRequest = TransferRequest.builder()
                .sourceAccountNumber("123456789")
                .targetAccountNumber("987654321")
                .amount(new BigDecimal("100.00"))
                .description("Test transfer")
                .build();

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
    void getTransferHistory_ShouldReturnGroupedTransactions() {
        Long accountId = 1L;
        List<Transaction> transactions = Arrays.asList(transaction1, transaction2);

        when(transactionRepository.findAllByAccountId(accountId)).thenReturn(transactions);
        when(basicMapper.convertToResponse(transaction1, TransferResponse.class)).thenReturn(transferRequest1);
        when(basicMapper.convertToResponse(transaction2, TransferResponse.class)).thenReturn(transferRequest2);

        Map<String, Map<String, List<TransferResponse>>> result = transferService.getTransferHistory(accountId);
        assertNotNull(result);
        assertEquals(2, result.size());

        String dateKey1 = transaction1.getTransactionDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String amountKey1 = transaction1.getAmount().toString();

        assertTrue(result.containsKey(dateKey1));
        assertTrue(result.get(dateKey1).containsKey(amountKey1));
        assertEquals(1, result.get(dateKey1).get(amountKey1).size());
        assertEquals(transferRequest1, result.get(dateKey1).get(amountKey1).get(0));

        String dateKey2 = transaction2.getTransactionDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String amountKey2 = transaction2.getAmount().toString();

        assertTrue(result.containsKey(dateKey2));
        assertTrue(result.get(dateKey2).containsKey(amountKey2));
        assertEquals(1, result.get(dateKey2).get(amountKey2).size());
        assertEquals(transferRequest2, result.get(dateKey2).get(amountKey2).get(0));

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
    void hasSufficientFunds_shouldReturnTrueWhenEnoughBalance() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));

        assertTrue(transferService.hasSufficientFunds(1L, new BigDecimal(500)));
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
    void hasSufficientFunds_shouldThrowWhenAmountIsZeroOrNegative() {
        assertThrows(IllegalArgumentException.class, () -> {
            transferService.hasSufficientFunds(1L, BigDecimal.ZERO);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            transferService.hasSufficientFunds(1L, new BigDecimal(-100));
        });
    }

    @Test
    void scheduleTransfer_WithValidRequest_ShouldSaveScheduledTransfer() throws SchedulerException {
        when(accountRepository.findByAccountNumber("123456789")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber("987654321")).thenReturn(Optional.of(targetAccount));
        when(scheduledTransferRepository.save(any(ScheduledTransfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        transferService.scheduleTransfer(validRequest, futureDate);

        verify(accountRepository, times(1)).findByAccountNumber("123456789");
        verify(accountRepository, times(1)).findByAccountNumber("987654321");
        verify(scheduledTransferRepository, atLeastOnce()).save(any(ScheduledTransfer.class));
        verify(quartzScheduler, times(1)).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    @Test
    void scheduleTransfer_WithNonexistentSourceAccount_ShouldThrowException() {
        when(accountRepository.findByAccountNumber("123456789")).thenReturn(Optional.empty());

        TransferException exception = assertThrows(TransferException.class,
                () -> transferService.scheduleTransfer(validRequest, futureDate));

        assertEquals("Source account not found", exception.getMessage());
        verify(scheduledTransferRepository, never()).save(any());
    }

    @Test
    void scheduleTransfer_WithNonexistentTargetAccount_ShouldThrowException() {
        when(accountRepository.findByAccountNumber("123456789")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber("987654321")).thenReturn(Optional.empty());

        TransferException exception = assertThrows(TransferException.class,
                () -> transferService.scheduleTransfer(validRequest, futureDate));

        assertEquals("Target account not found", exception.getMessage());
        verify(scheduledTransferRepository, never()).save(any());
    }

    @Test
    void scheduleTransferWithQuartz_WhenSchedulingFails_ShouldMarkAsFailed() throws SchedulerException {
        ScheduledTransfer transfer = ScheduledTransfer.builder()
                .id(1L)
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .amount(validRequest.getAmount())
                .scheduledDate(futureDate)
                .status(ScheduledTransferStatus.PENDING)
                .build();

        when(quartzScheduler.scheduleJob(any(), any()))
                .thenThrow(new SchedulerException("Test exception"));

        TransferException exception = assertThrows(TransferException.class,
                () -> transferService.scheduleTransferWithQuartz(transfer));

        assertEquals("Failed to schedule transfer", exception.getMessage());
        verify(scheduledTransferRepository, times(1)).save(any(ScheduledTransfer.class));
        assertEquals(ScheduledTransferStatus.FAILED, transfer.getStatus());
    }

    @Test
    void buildJobDetail_ShouldCreateValidJobDetail() {
        ScheduledTransfer transfer = ScheduledTransfer.builder()
                .id(1L)
                .build();

        JobDetail jobDetail = transferService.buildJobDetail(transfer);

        assertNotNull(jobDetail);
        assertEquals("scheduled-transfers", jobDetail.getKey().getGroup());
        assertEquals(ScheduledTransferJob.class, jobDetail.getJobClass());
        assertEquals(1L, jobDetail.getJobDataMap().get("transferId"));
        assertTrue(jobDetail.isDurable());
    }

    @Test
    void buildJobTrigger_ShouldCreateValidTrigger() {
        JobDetail jobDetail = JobBuilder.newJob(ScheduledTransferJob.class)
                .withIdentity("test-job", "test-group")
                .build();

        LocalDateTime triggerTime = LocalDateTime.now().plusHours(1);

        Trigger trigger = transferService.buildJobTrigger(jobDetail, triggerTime);

        assertNotNull(trigger);
        assertEquals("test-job", trigger.getKey().getName());
        assertEquals("scheduled-transfer-triggers", trigger.getKey().getGroup());
        assertEquals(jobDetail.getKey(), trigger.getJobKey());

        Date expectedDate = Date.from(triggerTime.atZone(ZoneId.systemDefault()).toInstant());
        assertEquals(expectedDate, trigger.getStartTime());
    }

    @Test
    void calculateTransferFee_ShouldThrowWhenAmountIsZeroOrNegative() {
        assertAll(
                () -> {
                    IllegalArgumentException exception = assertThrows(
                            IllegalArgumentException.class,
                            () -> transferService.calculateTransferFee(BigDecimal.ZERO, Currency.USD));
                    assertEquals("Amount must be greater than zero", exception.getMessage());
                },
                () -> {
                    IllegalArgumentException exception = assertThrows(
                            IllegalArgumentException.class,
                            () -> transferService.calculateTransferFee(new BigDecimal("-100"), Currency.USD));
                    assertEquals("Amount must be greater than zero", exception.getMessage());
                });
    }
}
