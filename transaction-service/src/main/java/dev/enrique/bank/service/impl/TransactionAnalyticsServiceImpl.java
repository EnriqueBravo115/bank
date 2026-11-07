package dev.enrique.bank.service.impl;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;

import dev.enrique.bank.commons.dto.response.TransactionResponse;
import dev.enrique.bank.commons.dto.response.TransactionSummaryResponse;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.config.CurrentCaller;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.service.TransactionAnalyticsService;
import lombok.RequiredArgsConstructor;

// Every report works on the transactions of one source identifier with one status; customers only
// see their own (CurrentCaller.ownerScope).
@Service
@RequiredArgsConstructor
public class TransactionAnalyticsServiceImpl implements TransactionAnalyticsService {
    private final TransactionRepository transactionRepository;
    private final CurrentCaller currentCaller;

    @Override
    public Map<TransactionType, List<TransactionResponse>> groupTransactionsByType(String sourceIdentifier,
            TransactionStatus status) {
        return find(sourceIdentifier, status).stream()
                .collect(groupingBy(TransactionResponse::transactionType));
    }

    @Override
    public Map<TransactionType, BigDecimal> sumTransactionsByType(String sourceIdentifier, TransactionStatus status) {
        return find(sourceIdentifier, status).stream()
                .collect(groupingBy(TransactionResponse::transactionType,
                        reducing(BigDecimal.ZERO, TransactionResponse::amount, BigDecimal::add)));
    }

    // true: amount greater than the threshold; false: less or equal
    @Override
    public Map<Boolean, List<TransactionResponse>> partitionTransactionsByAmount(String sourceIdentifier,
            TransactionStatus status, BigDecimal amount) {
        return find(sourceIdentifier, status).stream()
                .collect(partitioningBy(t -> t.amount().compareTo(amount) > 0));
    }

    @Override
    public Map<TransactionType, TransactionSummaryResponse> getTransactionTypeSummary(String sourceIdentifier,
            TransactionStatus status) {
        return find(sourceIdentifier, status).stream()
                .collect(groupingBy(TransactionResponse::transactionType,
                        collectingAndThen(toList(), list -> new TransactionSummaryResponse(
                                list.size(), total(list)))));
    }

    @Override
    public BigDecimal calculateTotalAmountByStatusAndType(String sourceIdentifier, TransactionStatus status,
            TransactionType type) {
        return total(transactionRepository.findAllBySourceIdentifierStatusAndType(currentCaller.ownerScope(), sourceIdentifier, status, type));
    }

    @Override
    public double getAverageDaysBetweenTransactions(String sourceIdentifier, TransactionStatus status) {
        List<TransactionResponse> sorted = find(sourceIdentifier, status).stream()
                .sorted(comparing(TransactionResponse::transactionDate))
                .toList();

        return IntStream.range(0, sorted.size() - 1)
                .mapToLong(i -> ChronoUnit.DAYS.between(
                        sorted.get(i).transactionDate().toLocalDate(),
                        sorted.get(i + 1).transactionDate().toLocalDate()))
                .average()
                .orElse(0);
    }

    @Override
    public Map<TransactionType, TransactionResponse> getMaxTransactionByType(String sourceIdentifier,
            TransactionStatus status) {
        return find(sourceIdentifier, status).stream()
                .collect(toMap(TransactionResponse::transactionType, Function.identity(),
                        BinaryOperator.maxBy(comparing(TransactionResponse::amount))));
    }

    @Override
    public Map<Month, Long> countTransactionsByMonth(String sourceIdentifier, TransactionStatus status) {
        return find(sourceIdentifier, status).stream()
                .collect(groupingBy(t -> t.transactionDate().getMonth(), counting()));
    }

    @Override
    public Map<TransactionType, BigDecimal> getAverageAmountByType(String sourceIdentifier, TransactionStatus status) {
        return find(sourceIdentifier, status).stream()
                .collect(groupingBy(TransactionResponse::transactionType,
                        collectingAndThen(toList(), list -> total(list)
                                .divide(BigDecimal.valueOf(list.size()), RoundingMode.HALF_UP))));
    }

    private List<TransactionResponse> find(String sourceIdentifier, TransactionStatus status) {
        return transactionRepository.findAllBySourceIdentifierAndStatus(currentCaller.ownerScope(), sourceIdentifier, status);
    }

    private static BigDecimal total(List<TransactionResponse> transactions) {
        return transactions.stream().map(TransactionResponse::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
