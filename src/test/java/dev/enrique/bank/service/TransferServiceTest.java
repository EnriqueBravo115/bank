package dev.enrique.bank.service;

import static dev.enrique.bank.commons.constants.ErrorMessage.ACCOUNT_NOT_FOUND;
import static dev.enrique.bank.commons.constants.ErrorMessage.INSUFFICIENT_FUNDS;
import static dev.enrique.bank.commons.constants.ErrorMessage.SCHEDULED_TRANSFER_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.http.HttpStatus;

import dev.enrique.bank.commons.enums.ScheduledTransferStatus;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.exception.ApiRequestException;
import dev.enrique.bank.dao.AccountRepository;
import dev.enrique.bank.dao.ScheduledTransferRepository;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.dto.request.TransferRequest;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.model.ScheduledTransfer;
import dev.enrique.bank.model.Transaction;
import dev.enrique.bank.service.impl.TransferServiceImpl;
import dev.enrique.bank.service.util.TransferHelper;

@ExtendWith(MockitoExtension.class)
public class TransferServiceTest {
    @Mock
    private ScheduledTransferRepository scheduledTransferRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransferHelper transferHelper;
    @Mock
    private Scheduler quartzScheduler;

    @InjectMocks
    private TransferServiceImpl transferService;

    private Account sourceAccount;
    private Account targetAccount;

    @BeforeEach
    void setUp() {
        sourceAccount = Account.builder()
                .id(1L)
                .accountNumber("123")
                .balance(new BigDecimal("200.00"))
                .build();

        targetAccount = Account.builder()
                .id(2L)
                .accountNumber("456")
                .balance(new BigDecimal("100.00"))
                .build();
    }

    @Test
    void transfer_ShouldCompleteSuccessfully_WhenValidData() {
        BigDecimal amount = new BigDecimal("100.00");

        when(accountRepository.findById(sourceAccount.getId())).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(targetAccount.getId())).thenReturn(Optional.of(targetAccount));

        transferService.transfer(sourceAccount.getId(), targetAccount.getId(), amount);

        assertEquals(new BigDecimal("100.00"), sourceAccount.getBalance());
        assertEquals(new BigDecimal("200.00"), targetAccount.getBalance());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void transfer_ShouldThrowException_WhenSourceAccountNotFound() {
        Long sourceAccountId = 1L;
        Long targetAccountId = 2L;
        BigDecimal amount = new BigDecimal("100.00");

        when(accountRepository.findById(sourceAccountId)).thenReturn(Optional.empty());

        ApiRequestException exception = assertThrows(ApiRequestException.class,
                () -> transferService.transfer(sourceAccountId, targetAccountId, amount));

        assertEquals(ACCOUNT_NOT_FOUND, exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void transfer_ShouldThrowException_WhenInsufficientFunds() {
        BigDecimal amount = new BigDecimal("300.00");

        when(accountRepository.findById(sourceAccount.getId())).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(targetAccount.getId())).thenReturn(Optional.of(targetAccount));

        ApiRequestException exception = assertThrows(ApiRequestException.class,
                () -> transferService.transfer(sourceAccount.getId(), targetAccount.getId(), amount));

        assertEquals(INSUFFICIENT_FUNDS, exception.getMessage());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
    }

    @Test
    void reverseTransfer_ShouldCompleteSuccessfully_WhenValidTransaction() {
        Long transactionId = 1L;
        BigDecimal amount = new BigDecimal("50.00");

        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .amount(amount)
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .transactionStatus(TransactionStatus.COMPLETED)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        transferService.reverseTransfer(transactionId);

        assertEquals(new BigDecimal("250.00"), sourceAccount.getBalance());
        assertEquals(new BigDecimal("50.00"), targetAccount.getBalance());
        assertEquals(TransactionStatus.REVERSED, transaction.getTransactionStatus());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void scheduleTransfer_ShouldScheduleSuccessfully_WhenValidRequest() throws SchedulerException {
        TransferRequest request = TransferRequest.builder()
                .sourceAccountNumber("123")
                .targetAccountNumber("456")
                .amount(new BigDecimal("100.00"))
                .description("Test transfer")
                .build();

        LocalDateTime scheduleDate = LocalDateTime.now().plusDays(1);

        when(accountRepository.findByAccountNumber("123")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber("456")).thenReturn(Optional.of(targetAccount));
        when(scheduledTransferRepository.save(any(ScheduledTransfer.class))).thenAnswer(invocation -> {
            ScheduledTransfer st = invocation.getArgument(0);
            st.setId(1L);
            return st;
        });

        when(quartzScheduler.scheduleJob(any(JobDetail.class), any(Trigger.class))).thenReturn(null);

        transferService.scheduleTransfer(request, scheduleDate);

        verify(scheduledTransferRepository).save(any(ScheduledTransfer.class));
        verify(quartzScheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    @Test
    void cancelScheduledTransfer_ShouldCancelSuccessfully_WhenTransferIsPending() {
        Long scheduledTransferId = 1L;

        ScheduledTransfer scheduledTransfer = new ScheduledTransfer();
        scheduledTransfer.setId(scheduledTransferId);
        scheduledTransfer.setStatus(ScheduledTransferStatus.PENDING);
        scheduledTransfer.setScheduledDate(LocalDateTime.now().plusDays(1));

        when(scheduledTransferRepository.findById(scheduledTransferId)).thenReturn(Optional.of(scheduledTransfer));

        transferService.cancelScheduledTransfer(scheduledTransferId);

        assertEquals(ScheduledTransferStatus.CANCELLED, scheduledTransfer.getStatus());
        assertNotNull(scheduledTransfer.getCancellationDate());
    }

    @Test
    void cancelScheduledTransfer_ShouldThrowException_WhenTransferNotFound() {
        Long scheduledTransferId = 1L;

        when(scheduledTransferRepository.findById(scheduledTransferId)).thenReturn(Optional.empty());

        ApiRequestException exception = assertThrows(ApiRequestException.class,
                () -> transferService.cancelScheduledTransfer(scheduledTransferId));

        assertEquals(SCHEDULED_TRANSFER_NOT_FOUND, exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }
}
