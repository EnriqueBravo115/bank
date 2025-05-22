package dev.enrique.bank.service.impl;

import static dev.enrique.bank.commons.constants.ErrorMessage.ACCOUNT_NOT_FOUND;
import static dev.enrique.bank.commons.constants.ErrorMessage.FAILED_SCHEDULED_TRANSFER;
import static dev.enrique.bank.commons.constants.ErrorMessage.INSUFFICIENT_FUNDS;
import static dev.enrique.bank.commons.constants.ErrorMessage.SCHEDULED_TRANSFER_NOT_FOUND;
import static dev.enrique.bank.commons.constants.ErrorMessage.TRANSACTION_NOT_FOUND;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.enrique.bank.commons.enums.ScheduledTransferStatus;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.commons.exception.ApiRequestException;
import dev.enrique.bank.config.ScheduledTransferJob;
import dev.enrique.bank.dao.AccountRepository;
import dev.enrique.bank.dao.ScheduledTransferRepository;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.dto.request.TransferRequest;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.model.ScheduledTransfer;
import dev.enrique.bank.model.Transaction;
import dev.enrique.bank.service.TransferService;
import dev.enrique.bank.service.util.TransferHelper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {
    private final ScheduledTransferRepository scheduledTransferRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransferHelper transferHelper;
    private final Scheduler quartzScheduler;

    @Override
    @Transactional
    public void transfer(Long sourceAccountId, Long targetAccountId, BigDecimal amount) {
        Account sourceAccount = accountRepository.findById(sourceAccountId)
                .orElseThrow(() -> new ApiRequestException(ACCOUNT_NOT_FOUND, HttpStatus.NOT_FOUND));

        Account targetAccount = accountRepository.findById(targetAccountId)
                .orElseThrow(() -> new ApiRequestException(ACCOUNT_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (sourceAccount.getBalance().compareTo(amount) < 0)
            throw new ApiRequestException(INSUFFICIENT_FUNDS, HttpStatus.UNPROCESSABLE_ENTITY);

        sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
        targetAccount.setBalance(targetAccount.getBalance().add(amount));

        Transaction transaction = Transaction.builder()
                .amount(amount)
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .transactionType(TransactionType.TRANSFER)
                .description("Transfer between accounts")
                .transactionDate(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public void reverseTransfer(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ApiRequestException(TRANSACTION_NOT_FOUND, HttpStatus.NOT_FOUND));

        Account sourceAccount = transaction.getSourceAccount();
        Account targetAccount = transaction.getTargetAccount();

        transferHelper.validateReverseTransfer(transaction, sourceAccount, targetAccount);

        Transaction reversal = Transaction.builder()
                .sourceAccount(targetAccount)
                .targetAccount(sourceAccount)
                .amount(transaction.getAmount())
                .transactionType(TransactionType.REVERSAL)
                .transactionStatus(TransactionStatus.PENDING)
                .originalTransaction(transaction)
                .build();

        sourceAccount.setBalance(sourceAccount.getBalance().add(transaction.getAmount()));
        targetAccount.setBalance(targetAccount.getBalance().subtract(transaction.getAmount()));

        transaction.setTransactionStatus(TransactionStatus.REVERSED);
        transactionRepository.save(reversal);
    }

    @Override
    @Transactional
    public void scheduleTransfer(TransferRequest request, LocalDateTime scheduleDate) {
        Account sourceAccount = accountRepository.findByAccountNumber(request.getSourceAccountNumber())
                .orElseThrow(() -> new ApiRequestException(ACCOUNT_NOT_FOUND, HttpStatus.NOT_FOUND));

        Account targetAccount = accountRepository.findByAccountNumber(request.getTargetAccountNumber())
                .orElseThrow(() -> new ApiRequestException(ACCOUNT_NOT_FOUND, HttpStatus.NOT_FOUND));

        transferHelper.validateTransferRequestAndAccounts(request, scheduleDate, sourceAccount, targetAccount,
                request.getAmount());

        ScheduledTransfer scheduledTransfer = ScheduledTransfer.builder()
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .amount(request.getAmount())
                .description(request.getDescription())
                .scheduledDate(scheduleDate)
                .status(ScheduledTransferStatus.PENDING)
                .creationDate(LocalDateTime.now())
                .build();

        scheduledTransferRepository.save(scheduledTransfer);
        scheduleTransferWithQuartz(scheduledTransfer);
    }

    @Override
    @Transactional
    public void cancelScheduledTransfer(Long scheduledTransferId) {
        ScheduledTransfer scheduledTransfer = scheduledTransferRepository.findById(scheduledTransferId)
                .orElseThrow(() -> new ApiRequestException(SCHEDULED_TRANSFER_NOT_FOUND, HttpStatus.NOT_FOUND));

        transferHelper.validateCancelScheduled(scheduledTransferId, scheduledTransfer);

        scheduledTransfer.setStatus(ScheduledTransferStatus.CANCELLED);
        scheduledTransfer.setCancellationDate(LocalDateTime.now());
    }

    @Transactional
    private void scheduleTransferWithQuartz(ScheduledTransfer scheduledTransfer) {
        try {
            JobDetail jobDetail = buildJobDetail(scheduledTransfer);
            Trigger trigger = buildJobTrigger(jobDetail, scheduledTransfer.getScheduledDate());

            quartzScheduler.scheduleJob(jobDetail, trigger);
            scheduledTransfer.setQuartzJobId(jobDetail.getKey().getName());

        } catch (SchedulerException e) {
            scheduledTransfer.setStatus(ScheduledTransferStatus.FAILED);
            throw new ApiRequestException(FAILED_SCHEDULED_TRANSFER, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private JobDetail buildJobDetail(ScheduledTransfer scheduledTransfer) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("transferId", scheduledTransfer.getId());

        return JobBuilder.newJob(ScheduledTransferJob.class)
                .withIdentity(UUID.randomUUID().toString(), "scheduled-transfers")
                .withDescription("Execute Scheduled Transfer")
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail, LocalDateTime startAt) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), "scheduled-transfer-triggers")
                .withDescription("Scheduled Transfer Trigger")
                .startAt(Date.from(startAt.atZone(ZoneId.systemDefault()).toInstant()))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
                .build();
    }
}
