package dev.enrique.bank.service.impl;

import static dev.enrique.bank.service.util.TransferHelper.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.ScheduledTransferStatus;
import dev.enrique.bank.commons.enums.Status;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.commons.exception.ScheduledTransferNotFoundException;
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
import dev.enrique.bank.service.TransferService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final ScheduledTransferRepository scheduledTransferRepository;
    private final BasicMapper basicMapper;
    private final Scheduler quartzScheduler;

    // Transfer history grouped in two levels(date -> amount),
    @Override
    public Map<String, Map<String, List<TransferResponse>>> getTransferHistory(Long accountId) {
        return transactionRepository.findAllByAccountId(accountId).stream()
                .collect(twoLevelGroupingBy(
                        t -> t.getTransactionDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        t -> t.getAmount().toString(),
                        t -> basicMapper.convertToResponse(t, TransferResponse.class)));
    }

    // List of transactions by year and sorted
    @Override
    public List<Transaction> getTransactionByYear(Integer year) {
        return transactionRepository.findAll()
                .stream()
                .filter(transaction -> year == null || transaction.getTransactionDate().getYear() == year)
                .sorted()
                .collect(Collectors.toList());
    }

    // podria usar cache
    @Override
    public Page<Transaction> getAllTransfers(Pageable pageable) {
        if (pageable == null)
            throw new IllegalArgumentException("Pageable cannot be null");

        Page<Transaction> transactionsPage = transactionRepository.findAll(pageable);

        if (transactionsPage.isEmpty())
            throw new TransferException("No transactions found");

        return transactionsPage;
    }

    @Override
    @Transactional
    public void transfer(Long sourceAccountId, Long targetAccountId, BigDecimal amount) {
        Account sourceAccount = accountRepository.findById(sourceAccountId)
                .orElseThrow(() -> new TransferException("Source account not found"));

        Account targetAccount = accountRepository.findById(targetAccountId)
                .orElseThrow(() -> new TransferException("Source account not found"));

        if (sourceAccount.getBalance().compareTo(amount) < 0)
            throw new TransferException("Insuficient funds");

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
                .orElseThrow(() -> new TransferException("Transaction not found with id: " + transactionId));

        if (transaction.getTransactionStatus() == TransactionStatus.REVERSED)
            throw new IllegalStateException("Transaction is already reversed");

        if (transaction.getTransactionStatus() != TransactionStatus.COMPLETED)
            throw new IllegalStateException("Only completed can be reversed");

        Account sourceAccount = transaction.getSourceAccount();
        Account targetAccount = transaction.getTargetAccount();

        if (sourceAccount.getStatus() != Status.OPEN || targetAccount.getStatus() != Status.OPEN)
            throw new IllegalStateException("Both accounts must be open to reverse the transaction");

        if (targetAccount.getBalance().compareTo(transaction.getAmount()) < 0)
            throw new IllegalStateException("Insufficient balance in target account to reverse transaction");

        Transaction reversal = Transaction.builder()
                .sourceAccount(targetAccount)
                .targetAccount(sourceAccount)
                .amount(transaction.getAmount())
                .transactionType(TransactionType.REVERSAL)
                .transactionStatus(TransactionStatus.PENDING)
                .originalTransaction(transaction)
                .build();

        sourceAccount.increaseBalance(transaction.getAmount());
        targetAccount.reduceBalance(transaction.getAmount());

        accountRepository.save(sourceAccount);
        accountRepository.save(targetAccount);

        transaction.setTransactionStatus(TransactionStatus.REVERSED);
        transactionRepository.save(transaction);
        transactionRepository.save(reversal);
    }

    @Override
    public boolean hasSufficientFunds(Long accountId, BigDecimal amount) {
        if (accountId == null)
            throw new IllegalArgumentException("Account ID cannot be null");

        if (amount == null)
            throw new IllegalArgumentException("Amount cannot be null");

        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Amount must be greater than zero");

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new TransferException("Account not found with id: " + accountId));

        if (account.getStatus() != Status.OPEN)
            throw new IllegalStateException("Account is not open");

        return account.getBalance().compareTo(amount) >= 0;
    }

    @Override
    public void scheduleTransfer(TransferRequest request, LocalDateTime scheduleDate) {
        Account sourceAccount = accountRepository.findByAccountNumber(request.getSourceAccountNumber())
                .orElseThrow(() -> new TransferException("Source account not found"));

        Account targetAccount = accountRepository.findByAccountNumber(request.getTargetAccountNumber())
                .orElseThrow(() -> new TransferException("Target account not found"));

        validateTransferRequestAndAccounts(request, scheduleDate, sourceAccount, targetAccount,
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
    public void cancelScheduledTransfer(Long scheduledTransferId) {
        ScheduledTransfer scheduledTransfer = scheduledTransferRepository.findById(scheduledTransferId)
                .orElseThrow(() -> new ScheduledTransferNotFoundException(
                        "Scheduled transfer not found with id: " + scheduledTransferId));

        validateCancelScheduled(scheduledTransferId, scheduledTransfer);

        scheduledTransfer.setStatus(ScheduledTransferStatus.CANCELLED);
        scheduledTransfer.setCancellationDate(LocalDateTime.now());

        scheduledTransferRepository.save(scheduledTransfer);
    }

    @Override
    public BigDecimal calculateTransferFee(BigDecimal amount, Currency currency) {
        if (amount == null)
            throw new IllegalArgumentException("Amount cannot be null");

        if (currency == null)
            throw new IllegalArgumentException("Currency cannot be null or empty");

        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Amount must be greater than zero");

        BigDecimal feePercentage = currency.getFeePercentage();
        BigDecimal minimumFee = currency.getMinimumFee();
        BigDecimal maximumFee = currency.getMaximumFee();

        BigDecimal calculatedFee = amount.multiply(feePercentage);

        if (calculatedFee.compareTo(minimumFee) < 0) {
            calculatedFee = minimumFee;
        } else if (calculatedFee.compareTo(maximumFee) > 0) {
            calculatedFee = maximumFee;
        }

        calculatedFee = calculatedFee.setScale(2, RoundingMode.HALF_UP);

        return calculatedFee;
    }

    @Override
    public BigDecimal getTransferLimit(Long accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new TransferException("Account not found with id: " + accountId));

        if (account.getStatus() != Status.OPEN) {
            throw new IllegalStateException("Account is not open");
        }

        return account.getBalance().multiply(new BigDecimal("2"));
    }

    @Override
    public void notifyTransfer(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransferException("Transaction not found with id: " + transactionId));

        // En una implementación real, esto enviaría notificaciones a los usuarios
        // Aquí solo simulamos la lógica básica
        String sourceMessage = String.format("Transfer of %s from your account %s has been processed",
                transaction.getAmount(),
                transaction.getSourceAccount().getAccountNumber());

        String targetMessage = String.format("Transfer of %s to your account %s has been received",
                transaction.getAmount(),
                transaction.getTargetAccount().getAccountNumber());

        // En una implementación real, llamaríamos a un servicio de notificación
        System.out.println("Notification to source account: " + sourceMessage);
        System.out.println("Notification to target account: " + targetMessage);

        // Marcamos la transacción como notificada
        transaction.setNotified(true);
        transactionRepository.save(transaction);
    }

    @Override
    public void validateTransfer(TransferRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Transfer request cannot be null");
        }

        if (request.getSourceAccountNumber() == null || request.getTargetAccountNumber() == null) {
            throw new IllegalArgumentException("Account numbers cannot be null");
        }

        if (request.getSourceAccountNumber().equals(request.getTargetAccountNumber())) {
            throw new IllegalArgumentException("Source and target accounts cannot be the same");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        Account sourceAccount = accountRepository.findByAccountNumber(request.getSourceAccountNumber())
                .orElseThrow(() -> new TransferException("Source account not found"));

        Account targetAccount = accountRepository.findByAccountNumber(request.getTargetAccountNumber())
                .orElseThrow(() -> new TransferException("Target account not found"));

        validateTransferRequestAndAccounts(request, LocalDateTime.now(), sourceAccount, targetAccount,
                request.getAmount());
    }

    public void scheduleTransferWithQuartz(ScheduledTransfer scheduledTransfer) {
        try {
            JobDetail jobDetail = buildJobDetail(scheduledTransfer);
            Trigger trigger = buildJobTrigger(jobDetail, scheduledTransfer.getScheduledDate());

            quartzScheduler.scheduleJob(jobDetail, trigger);

            scheduledTransfer.setQuartzJobId(jobDetail.getKey().getName());
            scheduledTransferRepository.save(scheduledTransfer);

        } catch (SchedulerException e) {
            scheduledTransfer.setStatus(ScheduledTransferStatus.FAILED);
            scheduledTransferRepository.save(scheduledTransfer);
            throw new TransferException("Failed to schedule transfer", e);
        }
    }

    public JobDetail buildJobDetail(ScheduledTransfer scheduledTransfer) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("transferId", scheduledTransfer.getId());

        return JobBuilder.newJob(ScheduledTransferJob.class)
                .withIdentity(UUID.randomUUID().toString(), "scheduled-transfers")
                .withDescription("Execute Scheduled Transfer")
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    public Trigger buildJobTrigger(JobDetail jobDetail, LocalDateTime startAt) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), "scheduled-transfer-triggers")
                .withDescription("Scheduled Transfer Trigger")
                .startAt(Date.from(startAt.atZone(ZoneId.systemDefault()).toInstant()))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
                .build();
    }
}
