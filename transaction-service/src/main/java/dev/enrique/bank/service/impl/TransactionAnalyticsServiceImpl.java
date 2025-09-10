package dev.enrique.bank.service.impl;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.summarizingDouble;
import static java.util.stream.Collectors.summarizingInt;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;

import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.dao.projection.TransactionCommonProjection;
import dev.enrique.bank.dao.projection.TransactionDetailedProjection;
import dev.enrique.bank.dto.response.TransactionCommonResponse;
import dev.enrique.bank.dto.response.TransactionDetailedResponse;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.model.Transaction;
import dev.enrique.bank.service.TransactionAnalyticsService;
import dev.enrique.bank.service.util.BasicMapper;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class TransactionAnalyticsServiceImpl implements TransactionAnalyticsService {
    private final TransactionRepository transactionRepository;
    private final BasicMapper basicMapper;

    @Override
    public Map<TransactionType, List<TransactionDetailedResponse>> groupTransactionsByType(Long accountId) {
        Map<TransactionType, List<TransactionDetailedProjection>> projections = transactionRepository
                .findCompletedByAccountId(accountId, TransactionDetailedProjection.class).stream()
                .collect(groupingBy(TransactionDetailedProjection::getTransactionType));

        return basicMapper.convertToTypedResponseMap(projections, TransactionDetailedResponse.class);
    }

    @Override
    public Map<TransactionType, BigDecimal> sumTransactionsByType(Long accountId) {
        return transactionRepository.findAllByAccountId(accountId).stream()
                .collect(groupingBy(Transaction::getTransactionType,
                        reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));
    }

    @Override
    public IntSummaryStatistics getTransactionYearStatistics(Long accountId) {
        return transactionRepository.findAllByAccountId(accountId).stream()
                .collect(summarizingInt(t -> t.getTransactionDate().getYear()));
    }

    @Override
    public Map<Boolean, List<TransactionCommonResponse>> partitionTransactionsByAmount(Long accountId,
            BigDecimal amount) {
        Map<Boolean, List<TransactionCommonProjection>> projection = transactionRepository
                .findCompletedByAccountId(accountId, TransactionCommonProjection.class).stream()
                .collect(partitioningBy(t -> t.getAmount().compareTo(amount) > 0));
        return basicMapper.convertToBooleanKeyResponseMap(projection, TransactionCommonResponse.class);
    }

    @Override
    public Map<TransactionType, String> getTransactionTypeSummary(Long accountId) {
        return transactionRepository.findAllByAccountId(accountId).stream()
                .collect(groupingBy(
                        Transaction::getTransactionType,
                        collectingAndThen(
                                summarizingDouble(t -> t.getAmount().doubleValue()),
                                stats -> String.format("Count: %d, Total: $%,.2f",
                                        stats.getCount(), stats.getSum()))));
    }

    @Override
    public BigDecimal calculateTotalTransactionAmount(Long accountId) {
        return transactionRepository.findAllByAccountId(accountId).stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal calculateTotalAmountByType(Long accountId, TransactionType type) {
        return transactionRepository.findAllByAccountId(accountId).stream()
                .filter(t -> t.getTransactionType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public double getAverageDaysBetweenTransactions(Long accountId) {
        List<Transaction> transactions = transactionRepository.findAllByAccountId(accountId);

        if (transactions.size() < 2)
            return 0;

        transactions.sort(comparing(Transaction::getTransactionDate));

        return IntStream.range(0, transactions.size() - 1)
                .mapToLong(i -> {
                    LocalDateTime date1 = transactions.get(i).getTransactionDate();
                    LocalDateTime date2 = transactions.get(i + 1).getTransactionDate();
                    return Duration.between(date1, date2).toDays();
                })
                .average()
                .orElse(0);
    }
}
