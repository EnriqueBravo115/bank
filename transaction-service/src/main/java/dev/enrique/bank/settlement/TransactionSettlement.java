package dev.enrique.bank.settlement;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dev.enrique.bank.client.AccountClient;
import dev.enrique.bank.commons.dto.request.ClientDepositRequest;
import dev.enrique.bank.commons.dto.request.ClientPurchaseRequest;
import dev.enrique.bank.commons.dto.request.ClientServiceRequest;
import dev.enrique.bank.commons.dto.request.ClientTransferRequest;
import dev.enrique.bank.commons.dto.request.ClientWithdrawalRequest;
import dev.enrique.bank.commons.dto.response.MovementResultResponse;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.exception.TransactionNotFoundException;
import dev.enrique.bank.commons.exception.TransactionNotReversibleException;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Moves the money of a transaction in account-service without losing track of it:
 * <ol>
 * <li>the transaction is committed as PENDING before calling account-service;</li>
 * <li>the call carries the transaction code as Idempotency-Key;</li>
 * <li>a final answer (COMPLETED, DECLINED, FAILED) is stored; an unknown one (timeout, 5xx, open
 * circuit) leaves it PENDING and {@link #retryPending} sends the same request again with backoff.</li>
 * </ol>
 * account-service applies each key once, so retrying never moves the money twice. After
 * {@link #MAX_ATTEMPTS} the transaction is left PENDING without a next attempt ("stuck") for an admin,
 * who can {@link #retryStuck retry it} or cancel it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionSettlement {
    static final Duration FIRST_RETRY_DELAY = Duration.ofSeconds(5);
    static final Duration MAX_RETRY_DELAY = Duration.ofMinutes(15);
    static final int BATCH_SIZE = 50;
    // About 3 hours of attempts (5s doubling up to 15 min); then an admin decides (retry or cancel).
    public static final int MAX_ATTEMPTS = 20;

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;

    public Transaction register(Transaction transaction) {
        LocalDateTime now = LocalDateTime.now();
        transaction.setTransactionCode(UUID.randomUUID().toString());
        transaction.setTransactionDate(now);
        transaction.setTransactionStatus(TransactionStatus.PENDING);
        transaction.setReason(AccountClient.AWAITING_CONFIRMATION);
        transaction.setNextAttemptAt(now);
        Transaction pending = transactionRepository.save(transaction);

        return settle(pending, now);
    }

    @Transactional
    public int retryPending() {
        return retryPending(LocalDateTime.now());
    }

    // FOR UPDATE SKIP LOCKED: several replicas never retry the same transaction at once.
    @Transactional
    public int retryPending(LocalDateTime now) {
        List<Transaction> due = transactionRepository.lockPending(now, BATCH_SIZE);
        due.forEach(transaction -> settle(transaction, now));
        return due.size();
    }

    // One more attempt for a stuck transaction, with a fresh round of automatic retries if it fails again.
    @Transactional
    public Transaction retryStuck(String transactionCode) {
        Transaction transaction = transactionRepository.lockByTransactionCode(transactionCode)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Transaction not found with code: " + transactionCode));
        if (!transaction.isStuck()) {
            throw new TransactionNotReversibleException("Only PENDING transactions without automatic attempts "
                    + "left can be retried, this one is " + transaction.getTransactionStatus());
        }
        transaction.setAttempts(0);
        return settle(transaction, LocalDateTime.now());
    }

    private Transaction settle(Transaction transaction, LocalDateTime now) {
        MovementResultResponse result = send(transaction);
        // The wait starts when the attempt ends: a call that timed out is not retried right away.
        LocalDateTime finished = LocalDateTime.now();
        LocalDateTime base = now.isAfter(finished) ? now : finished;
        transaction.registerAttempt(result.transactionStatus(), result.reason(),
                base.plus(nextDelay(transaction.getAttempts() + 1)));

        if (transaction.isPending() && transaction.getAttempts() >= MAX_ATTEMPTS) {
            transaction.stopRetrying("No answer from account-service after " + transaction.getAttempts()
                    + " attempts (" + result.reason() + "): waiting for an admin to retry or cancel it");
            log.error("Transaction {} is stuck PENDING after {} attempts", transaction.getTransactionCode(),
                    transaction.getAttempts());
        } else if (transaction.isPending()) {
            log.warn("Transaction {} still PENDING after attempt {}: {}", transaction.getTransactionCode(),
                    transaction.getAttempts(), result.reason());
        } else {
            log.info("Transaction {} ({}) settled as {}: {}", transaction.getTransactionCode(),
                    transaction.getTransactionType(), result.transactionStatus(), result.reason());
        }
        return transactionRepository.save(transaction);
    }

    private MovementResultResponse send(Transaction transaction) {
        String key = transaction.getTransactionCode();
        return switch (transaction.getTransactionType()) {
            case TRANSFER -> accountClient.processTransfer(key, new ClientTransferRequest(
                    transaction.getAmount(),
                    transaction.getAccountNumber(),
                    transaction.getTransferTransaction().getTargetAccountNumber(),
                    transaction.getCurrency()));
            case PURCHASE -> accountClient.processPurchase(key, new ClientPurchaseRequest(
                    transaction.getAccountNumber(),
                    transaction.getAmount(),
                    transaction.getPurchaseTransaction().getSourceCardNumber(),
                    transaction.getPurchaseTransaction().getCvv(),
                    transaction.getCurrency()));
            case SERVICE -> accountClient.processService(key, new ClientServiceRequest(
                    transaction.getAccountNumber(),
                    transaction.getAmount(),
                    transaction.getCurrency()));
            case WITHDRAWAL -> accountClient.processWithdrawal(key, new ClientWithdrawalRequest(
                    transaction.getAccountNumber(),
                    transaction.getAmount(),
                    transaction.getWithdrawalTransaction().getSecurityVerificationMethod(),
                    transaction.getWithdrawalTransaction().getTransactionFee(),
                    transaction.getCurrency()));
            // The deposited account is the transaction's account: there is no source account
            case DEPOSIT -> accountClient.processDeposit(key, new ClientDepositRequest(
                    transaction.getAccountNumber(),
                    transaction.getAmount(),
                    transaction.getCurrency()));
            case OTHER -> throw new IllegalStateException("Unsupported transaction type: OTHER");
        };
    }

    // 5s, 10s, 20s... up to 15 min between attempts.
    static Duration nextDelay(int attempt) {
        Duration delay = FIRST_RETRY_DELAY.multipliedBy(1L << Math.min(attempt - 1, 20));
        return delay.compareTo(MAX_RETRY_DELAY) > 0 ? MAX_RETRY_DELAY : delay;
    }
}
