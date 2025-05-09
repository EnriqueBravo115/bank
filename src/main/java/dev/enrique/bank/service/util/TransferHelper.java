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
import dev.enrique.bank.commons.exception.InsufficientFundsException;
import dev.enrique.bank.dto.request.TransferRequest;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.model.ScheduledTransfer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransferHelper {
    // Agrupa elementos en un mapa anidado de dos niveles(f1 -> f2), y devuelve
    // List<R>
    public static <T, A, B, R> Collector<T, ?, Map<A, Map<B, List<R>>>> twoLevelGroupingBy(
            Function<? super T, ? extends A> f1,
            Function<? super T, ? extends B> f2,
            Function<? super T, ? extends R> mapper) {

        Collector<T, ?, List<R>> downstream = mapping(mapper, toList());
        Collector<T, ?, Map<B, List<R>>> secondGrouping = groupingBy(f2, downstream);

        return groupingBy(f1, secondGrouping);
    }

    public static void validateTransferForExecution(ScheduledTransfer transfer) {
        if (transfer.getStatus() != ScheduledTransferStatus.PENDING) {
            throw new IllegalStateException("Transfer is not in PENDING state");
        }

        if (transfer.getSourceAccount().getStatus() != Status.OPEN ||
                transfer.getTargetAccount().getStatus() != Status.OPEN) {
            throw new IllegalStateException("One or both accounts are not OPEN");
        }

        if (transfer.getSourceAccount().getBalance().compareTo(transfer.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds in source account");
        }
    }

    public static void validateTransferRequestAndAccounts(TransferRequest request, LocalDateTime scheduleDate,
            Account source,
            Account target, BigDecimal amount) {
        if (request == null)
            throw new IllegalArgumentException("TransferRequest cannot be null");

        if (scheduleDate == null)
            throw new IllegalArgumentException("Schedule date cannot be null");

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

    public static void validateCancelScheduled(Long id, ScheduledTransfer scheduledTransfer) {
        if (id == null)
            throw new IllegalArgumentException("Scheduled transfer ID cannot be null");

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

    public static void validateHasSufficientFunds(){

    }
}
