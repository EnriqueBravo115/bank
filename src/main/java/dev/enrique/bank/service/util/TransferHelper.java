package dev.enrique.bank.service.util;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;

import org.springframework.stereotype.Component;

import dev.enrique.bank.commons.enums.ScheduledTransferStatus;
import dev.enrique.bank.commons.enums.Status;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.dto.request.TransferRequest;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.model.ScheduledTransfer;
import dev.enrique.bank.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransferHelper {
    public void validateTransferRequestAndAccounts(TransferRequest request,
            LocalDateTime scheduleDate,
            Account source,
            Account target, BigDecimal amount) {

        if (scheduleDate.isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("Schedule date cannot be in the past");

        if (source.getStatus() != Status.OPEN || target.getStatus() != Status.OPEN)
            throw new IllegalStateException("Both accounts must be open");

        if (source.getBalance().compareTo(amount) < 0)
            throw new IllegalStateException("Insufficient funds in source account");

        if (source.getStatus() != Status.OPEN || target.getStatus() != Status.OPEN)
            throw new IllegalStateException("Both accounts must be open");

        if (source.getBalance().compareTo(request.getAmount()) < 0)
            throw new IllegalStateException("Insufficient funds in source account");
    }

    public void validateCancelScheduled(Long id, ScheduledTransfer scheduledTransfer) {
        if (scheduledTransfer.getStatus() != ScheduledTransferStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING scheduled transfers can be cancelled. Current status: " +
                            scheduledTransfer.getStatus());
        }

        if (scheduledTransfer.getScheduledDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException(
                    "Cannot cancel a scheduled transfer that has already passed its execution date");
        }

        if (scheduledTransfer.getSourceAccount().getStatus() != Status.OPEN ||
                scheduledTransfer.getTargetAccount().getStatus() != Status.OPEN) {
            throw new IllegalStateException("Cannot cancel transfer because one or both accounts are not OPEN");
        }
    }

    public void validateReverseTransfer(Transaction transaction, Account sourceAccount, Account targetAccount) {
        if (transaction.getTransactionStatus() == TransactionStatus.REVERSED)
            throw new IllegalStateException("Transaction is already reversed");

        if (transaction.getTransactionStatus() != TransactionStatus.COMPLETED)
            throw new IllegalStateException("Only completed can be reversed");

        if (sourceAccount.getStatus() != Status.OPEN || targetAccount.getStatus() != Status.OPEN)
            throw new IllegalStateException("Both accounts must be open to reverse the transaction");

        if (targetAccount.getBalance().compareTo(transaction.getAmount()) < 0)
            throw new IllegalStateException("Insufficient balance in target account to reverse transaction");
    }
}
