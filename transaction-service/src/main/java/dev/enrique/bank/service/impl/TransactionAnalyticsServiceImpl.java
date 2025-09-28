package dev.enrique.bank.service.impl;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toList;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;

import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.dao.projection.TransactionBasicProjection;
import dev.enrique.bank.dao.projection.TransactionCommonProjection;
import dev.enrique.bank.dao.projection.TransactionDetailedProjection;
import dev.enrique.bank.commons.dto.response.TransactionCommonResponse;
import dev.enrique.bank.commons.dto.response.TransactionDetailedResponse;
import dev.enrique.bank.commons.dto.response.TransactionSummaryResponse;
import dev.enrique.bank.model.Transaction;
import dev.enrique.bank.service.TransactionAnalyticsService;
import dev.enrique.bank.commons.util.BasicMapper;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class TransactionAnalyticsServiceImpl implements TransactionAnalyticsService {
    private final TransactionRepository transactionRepository;
    private final BasicMapper basicMapper;

    // TODO: Implement polymorphism according to TransactionStatus
    // Groups transactions by type and converts projections into DTO responses,
    // ordered by date descending from the repository
    @Override
    public Map<TransactionType, List<TransactionDetailedResponse>> groupTransactionsByType(String accountNumber) {
        Map<TransactionType, List<TransactionDetailedProjection>> projections = transactionRepository
                .findAllByAccountNumber(accountNumber, TransactionDetailedProjection.class).stream()
                .collect(groupingBy(TransactionDetailedProjection::getTransactionType));

        return basicMapper.convertToTypedResponseMap(projections, TransactionDetailedResponse.class);
    }

    @Override
    public Map<TransactionType, BigDecimal> sumTransactionsByType(String accountNumber) {
        return transactionRepository.findAllCompletedByAccountNumber(accountNumber).stream()
                .collect(groupingBy(Transaction::getTransactionType,
                        reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));
    }

    // - TRUE: Transactions with amount greater than the specified amount
    // - FALSE: Transactions with amount less than or equal to the specified threshold
    @Override
    public Map<Boolean, List<TransactionCommonResponse>> partitionTransactionsByAmount(String accountNumber,
            BigDecimal amount) {
        Map<Boolean, List<TransactionCommonProjection>> projection = transactionRepository
                .findAllCompletedByAccountNumber(accountNumber, TransactionCommonProjection.class)
                .stream()
                .collect(partitioningBy(t -> t.getAmount().compareTo(amount) > 0));

        return basicMapper.convertToBooleanKeyResponseMap(projection, TransactionCommonResponse.class);
    }

    // Groups transactions by type and calculates a TransactionSummaryResponse with:
    // 1. The number of transactions
    // 2. The total amount
    public Map<TransactionType, TransactionSummaryResponse> getTransactionTypeSummary(String accountNumber) {
        return transactionRepository.findAllCompletedByAccountNumber(accountNumber, TransactionBasicProjection.class)
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

    @Override
    public BigDecimal calculateTotalTransactionAmount(String accountNumber) {
        return transactionRepository.findAllCompletedByAccountNumber(accountNumber, TransactionBasicProjection.class)
                .stream()
                .map(TransactionBasicProjection::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal calculateTotalAmountByType(String accountNumber, TransactionType type) {
        return transactionRepository.findAllCompletedByAccountNumber(accountNumber, TransactionBasicProjection.class)
                .stream()
                .filter(t -> t.getTransactionType() == type)
                .map(TransactionBasicProjection::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Calculates the average days between completed transactions for an account
    // - Uses ChronosUnit.DAYS.between() for date difference calculation
    @Override
    public double getAverageDaysBetweenTransactions(String accountNumber) {
        List<Transaction> sorted = transactionRepository.findAllCompletedByAccountNumber(accountNumber)
                .stream()
                .sorted(comparing(Transaction::getTransactionDate))
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
}
