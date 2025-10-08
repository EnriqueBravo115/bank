package dev.enrique.bank.service.impl;

import dev.enrique.bank.commons.dto.response.TransactionSummaryResponse;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.dao.projection.TransactionBasicProjection;
import dev.enrique.bank.dao.projection.TransactionCommonProjection;
import dev.enrique.bank.dao.projection.TransactionDetailedProjection;
import dev.enrique.bank.service.TransactionAnalyticsService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.*;

@Service
@AllArgsConstructor
public class TransactionAnalyticsServiceImpl implements TransactionAnalyticsService {
    private final TransactionRepository transactionRepository;

    // FIX: potential problem calculating TransferTransactions
    // Groups transactions by type and filter by status ordered by date 'DESC'
    @Override
    public Map<TransactionType, List<TransactionDetailedProjection>> groupTransactionsByType(String sourceIdentifier,
                                                                                             TransactionStatus status) {
        return transactionRepository
                .findAllBySourceIdentifierAndStatus(sourceIdentifier, status, TransactionDetailedProjection.class)
                .stream()
                .collect(groupingBy(TransactionDetailedProjection::getTransactionType));
    }

    // Calculates the total sum of all completed transactions grouped by their
    // transaction type, ordered by date 'DESC'
    @Override
    public Map<TransactionType, BigDecimal> sumTransactionsByType(String sourceIdentifier, TransactionStatus status) {
        return transactionRepository
                .findAllBySourceIdentifierAndStatus(sourceIdentifier, status, TransactionBasicProjection.class)
                .stream()
                .collect(groupingBy(TransactionBasicProjection::getTransactionType,
                        reducing(BigDecimal.ZERO, TransactionBasicProjection::getAmount, BigDecimal::add)));
    }

    // TRUE: Transactions with amount greater
    // FALSE: Transactions with amount less or equal
    @Override
    public Map<Boolean, List<TransactionBasicProjection>> partitionTransactionsByAmount(String accountNumber,
                                                                                        TransactionStatus status,
                                                                                        BigDecimal amount) {
        return transactionRepository
                .findAllBySourceIdentifierAndStatus(accountNumber, status, TransactionBasicProjection.class)
                .stream()
                .collect(partitioningBy(t -> t.getAmount().compareTo(amount) > 0));
    }

    // Groups transactions by type and calculates a TransactionSummaryResponse with:
    // 1. The number of transactions
    // 2. The total amount
    public Map<TransactionType, TransactionSummaryResponse> getTransactionTypeSummary(String sourceIdentifier,
                                                                                      TransactionStatus status) {
        return transactionRepository
                .findAllBySourceIdentifierAndStatus(sourceIdentifier, status, TransactionBasicProjection.class)
                .stream()
                .collect(groupingBy(
                        TransactionBasicProjection::getTransactionType,
                        collectingAndThen(
                                toList(),
                                list -> new TransactionSummaryResponse(
                                        list.size(),
                                        list.stream()
                                                .map(TransactionBasicProjection::getAmount)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add)))));
    }

    // Calculates the total amount of transactions for an account
    // filtered by status and type
    @Override
    public BigDecimal calculateTotalAmountByStatusAndType(String sourceIdentifier, TransactionStatus status,
                                                          TransactionType type) {
        return transactionRepository
                .findAllBySourceIdentifierStatusAndType(sourceIdentifier, status, type, TransactionBasicProjection.class)
                .stream()
                .map(TransactionBasicProjection::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Calculates the average days between completed transactions for an account
    // - Uses ChronosUnit.DAYS.between() for date difference calculation
    @Override
    public double getAverageDaysBetweenTransactions(String sourceIdentifier, TransactionStatus status) {
        List<TransactionCommonProjection> sorted = transactionRepository
                .findAllBySourceIdentifierAndStatus(sourceIdentifier, status, TransactionCommonProjection.class)
                .stream()
                .sorted(comparing(TransactionCommonProjection::getTransactionDate))
                .toList();

        if (sorted.size() < 2)
            return 0;

        return IntStream.range(0, sorted.size() - 1)
                .mapToLong(i -> ChronoUnit.DAYS.between(
                        sorted.get(i).getTransactionDate().toLocalDate(),
                        sorted.get(i + 1).getTransactionDate().toLocalDate()))
                .average()
                .orElseThrow(() -> new IllegalStateException("Cannot calculate average"));
    }

    // Returns the transaction with the highest amount for each transaction type
    @Override
    public Map<TransactionType, List<TransactionBasicProjection>> getMaxTransactionByType(String sourceIdentifier,
                                                                                          TransactionStatus status) {
        return transactionRepository
                .findAllBySourceIdentifierAndStatus(sourceIdentifier, status, TransactionBasicProjection.class)
                .stream()
                .collect(groupingBy(
                        TransactionBasicProjection::getTransactionType,
                        collectingAndThen(
                                maxBy(comparing(TransactionBasicProjection::getAmount)),
                                opt -> opt.map(List::of).orElseGet(List::of))));
    }

    // Counts the number of transactions per month for a given account and status
    @Override
    public Map<Month, Long> countTransactionsByMonth(String sourceIdentifier, TransactionStatus status) {
        return transactionRepository
                .findAllBySourceIdentifierAndStatus(sourceIdentifier, status, TransactionCommonProjection.class)
                .stream()
                .collect(groupingBy(t -> t.getTransactionDate().getMonth(), counting()));
    }

    // Calculates the average transaction amount grouped by transaction type
    @Override
    public Map<TransactionType, BigDecimal> getAverageAmountByType(String sourceIdentifier, TransactionStatus status) {
        Map<TransactionType, List<TransactionBasicProjection>> grouped = transactionRepository
                .findAllBySourceIdentifierAndStatus(sourceIdentifier, status, TransactionBasicProjection.class)
                .stream()
                .collect(groupingBy(TransactionBasicProjection::getTransactionType));

        return grouped.entrySet()
                .stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .map(TransactionBasicProjection::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .divide(BigDecimal.valueOf(e.getValue().size()), RoundingMode.HALF_UP)));
    }
}
