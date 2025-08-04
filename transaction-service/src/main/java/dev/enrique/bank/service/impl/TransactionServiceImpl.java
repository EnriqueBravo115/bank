package dev.enrique.bank.service.impl;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.averagingDouble;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.summarizingDouble;
import static java.util.stream.Collectors.summarizingInt;
import static java.util.stream.Collectors.toSet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import dev.enrique.bank.enums.Currency;
import dev.enrique.bank.enums.AccountStatus;
import dev.enrique.bank.enums.TransactionType;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.dao.projection.TransactionBasicProjection;
import dev.enrique.bank.dao.projection.TransactionCommonProjection;
import dev.enrique.bank.dao.projection.TransactionDetailedProjection;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.model.Transaction;
import dev.enrique.bank.service.TransactionService;
import dev.enrique.bank.service.util.AccountHelper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountHelper accountHelper;

    @Override
    public List<TransactionDetailedProjection> getTransactionHistory(Long accountId) {
        accountHelper.validateAccountId(accountId);
        return transactionRepository.findCompletedByAccountId(accountId, TransactionDetailedProjection.class);
    }

    @Override
    public Page<TransactionCommonProjection> getAllTransactions(Long accountId, Pageable pageable) {
        accountHelper.validateAccountId(accountId);
        return transactionRepository.findCompletedByAccountId(accountId, pageable);
    }

    @Override
    public List<TransactionCommonProjection> getTransactionByYearAndAccount(Long accountId, Integer year) {
        accountHelper.validateAccountIdAndYear(accountId, year);
        return transactionRepository.findByAccountIdAndYear(accountId, year);
    }

    @Override
    public List<TransactionBasicProjection> getAllTransactionsReversals(Long accountId) {
        accountHelper.validateAccountId(accountId);
        return transactionRepository.findReversalsByAccountId(accountId);
    }

    @Override
    public List<TransactionCommonProjection> getAllTransactionsFromAccounts(List<Long> accountIds) {
        accountIds.forEach(accountHelper::validateAccountId);
        return transactionRepository.findCompletedByAccountIdsIn(accountIds);
    }

    @Override
    public Map<TransactionType, List<TransactionCommonProjection>> groupTransactionsByType(Long accountId) {
        accountHelper.validateAccountId(accountId);
        return transactionRepository
                .findCompletedByAccountId(accountId, TransactionCommonProjection.class).stream()
                .collect(groupingBy(TransactionCommonProjection::getTransactionType));
    }

    @Override
    public Map<TransactionType, BigDecimal> sumTransactionsByType(Long accountId) {
        accountHelper.validateAccountId(accountId);
        return transactionRepository.findAllByAccountId(accountId).stream()
                .collect(groupingBy(Transaction::getTransactionType,
                        reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));
    }

    // cambiar el parametro a evaluar
    @Override
    public IntSummaryStatistics getTransactionYearStatistics(Long accountId) {
        accountHelper.validateAccountId(accountId);
        return transactionRepository.findAllByAccountId(accountId).stream()
                .collect(summarizingInt(t -> t.getTransactionDate().getYear()));
    }

    @Override
    public Map<Boolean, List<TransactionBasicProjection>> partitionTransactionsByAmount(Long accountId,
            BigDecimal amount) {
        accountHelper.validateAccountId(accountId);
        return transactionRepository.findCompletedByAccountId(accountId, TransactionBasicProjection.class).stream()
                .collect(partitioningBy(t -> t.getAmount().compareTo(amount) > 0));
    }

    @Override
    public Map<TransactionType, String> getTransactionTypeSummary(Long accountId) {
        accountHelper.validateAccountId(accountId);
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
        accountHelper.validateAccountId(accountId);
        return transactionRepository.findAllByAccountId(accountId).stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal calculateTotalAmountByType(Long accountId, TransactionType type) {
        accountHelper.validateAccountIdAndTransactionType(accountId, type);
        return transactionRepository.findAllByAccountId(accountId).stream()
                .filter(t -> t.getTransactionType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal calculateTransferFee(BigDecimal amount, Currency currency) {
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

        return calculatedFee.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public Boolean hasSufficientFunds(Long accountId, BigDecimal amount) {
        Account account = accountHelper.getAccountById(accountId);
        return account.getBalance().compareTo(amount) > 0;
    }

    // el limite sera 50,000 dls crear otro type de normal y deluxe
    @Override
    public BigDecimal getTransferLimit(Long accountId) {
        accountHelper.validateAccountId(accountId);
        Account account = accountHelper.getAccountById(accountId);

        if (account.getStatus() != AccountStatus.OPEN)
            throw new IllegalStateException("Account is not open");

        return account.getBalance().multiply(new BigDecimal("2"));
    }

    @Override
    public Set<String> getAllUniqueTransactionDescriptions(Long accountId) {
        accountHelper.validateAccountId(accountId);
        return transactionRepository.findAllByAccountId(accountId).stream()
                .map(Transaction::getDescription)
                .filter(Objects::nonNull)
                .filter(desc -> !desc.isBlank())
                .flatMap(desc -> Stream.of(desc.split(" ")))
                .filter(word -> !word.isBlank())
                .collect(toSet());
    }

    @Override
    public String getAllTransactionDescriptions(Long accountId) {
        accountHelper.validateAccountId(accountId);
        return transactionRepository.findAllByAccountId(accountId).stream()
                .map(Transaction::getDescription)
                .collect(joining(", "));
    }

    @Override
    public String getFormattedAverageBalance(List<Long> accountIds) {
        accountIds.forEach(accountHelper::validateAccountId);
        return accountIds.stream()
                .map(accountHelper::getAccountById)
                .collect(collectingAndThen(
                        averagingDouble(a -> a.getBalance().doubleValue()),
                        avg -> String.format("$%,.2f", avg)));
    }

    @Override
    public Optional<TransactionBasicProjection> findMaxTransaction(Long accountId) {
        accountHelper.validateAccountId(accountId);
        return transactionRepository.findCompletedByAccountId(accountId, TransactionBasicProjection.class).stream()
                .collect(reducing((t1, t2) -> t1.getAmount().compareTo(t2.getAmount()) > 0 ? t1 : t2));
    }

    @Override
    public double getAverageDaysBetweenTransactions(Long accountId) {
        accountHelper.validateAccountId(accountId);
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
