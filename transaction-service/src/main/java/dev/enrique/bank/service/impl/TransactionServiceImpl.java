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

import dev.enrique.bank.dto.response.TransactionBasicResponse;
import dev.enrique.bank.dto.response.TransactionCommonResponse;
import dev.enrique.bank.dto.response.TransactionDetailedResponse;
import dev.enrique.bank.mapper.BasicMapper;
import dev.enrique.bank.mapper.HeaderResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.dao.projection.TransactionBasicProjection;
import dev.enrique.bank.dao.projection.TransactionCommonProjection;
import dev.enrique.bank.dao.projection.TransactionDetailedProjection;
import dev.enrique.bank.enums.TransactionType;
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
    private final BasicMapper basicMapper;

    @Override
    public List<TransactionDetailedResponse> getTransactionHistory(Long accountId) {
        accountHelper.validateAccountId(accountId);
        List<TransactionDetailedProjection> projections = transactionRepository
                .findCompletedByAccountId(accountId, TransactionDetailedProjection.class);
        return basicMapper.convertToResponseList(projections, TransactionDetailedResponse.class);
    }

    @Override
    public HeaderResponse<TransactionCommonResponse> getAllTransactions(Long accountId, Pageable pageable) {
        accountHelper.validateAccountId(accountId);
        Page<TransactionCommonProjection> page = transactionRepository.findCompletedByAccountId(accountId, pageable);
        return basicMapper.getHeaderResponse(page, TransactionCommonResponse.class);
    }

    @Override
    public List<TransactionCommonResponse> getTransactionByYearAndAccount(Long accountId, Integer year) {
        accountHelper.validateAccountIdAndYear(accountId, year);
        List<TransactionCommonProjection> projections = transactionRepository.findByAccountIdAndYear(accountId, year);
        return basicMapper.convertToResponseList(projections, TransactionCommonResponse.class);
    }

    @Override
    public List<TransactionBasicResponse> getAllTransactionsReversals(Long accountId) {
        accountHelper.validateAccountId(accountId);
        List<TransactionBasicProjection> projections = transactionRepository.findReversalsByAccountId(accountId);
        return basicMapper.convertToResponseList(projections, TransactionBasicResponse.class);
    }

    @Override
    public List<TransactionCommonResponse> getAllTransactionsFromAccounts(List<Long> accountIds) {
        accountIds.forEach(accountHelper::validateAccountId);
        List<TransactionCommonProjection> projections = transactionRepository.findCompletedByAccountIdsIn(accountIds);
        return basicMapper.convertToResponseList(projections, TransactionCommonResponse.class);
    }

    @Override
    public Map<TransactionType, List<TransactionCommonResponse>> groupTransactionsByType(Long accountId) {
        accountHelper.validateAccountId(accountId);
        Map<TransactionType, List<TransactionCommonProjection>> projections = transactionRepository
                .findCompletedByAccountId(accountId, TransactionCommonProjection.class).stream()
                .collect(groupingBy(TransactionCommonProjection::getTransactionType));
        return basicMapper.convertToTypedResponseMap(projections, TransactionCommonResponse.class);
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
    public Map<Boolean, List<TransactionBasicResponse>> partitionTransactionsByAmount(Long accountId,
            BigDecimal amount) {
        accountHelper.validateAccountId(accountId);
        Map<Boolean, List<TransactionBasicProjection>> projection = transactionRepository
                .findCompletedByAccountId(accountId, TransactionBasicProjection.class).stream()
                .collect(partitioningBy(t -> t.getAmount().compareTo(amount) > 0));

        return basicMapper.convertToBooleanKeyResponseMap(projection, TransactionBasicResponse.class);
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
    public Boolean hasSufficientFunds(Long accountId, BigDecimal amount) {
        Account account = accountHelper.getAccountById(accountId);
        return account.getBalance().compareTo(amount) > 0;
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
    public Optional<TransactionBasicResponse> findMaxTransaction(Long accountId) {
        accountHelper.validateAccountId(accountId);
        Optional<TransactionBasicProjection> projection = transactionRepository
                .findCompletedByAccountId(accountId, TransactionBasicProjection.class).stream()
                .collect(reducing((t1, t2) -> t1.getAmount().compareTo(t2.getAmount()) > 0 ? t1 : t2));
        return basicMapper.convertOptionalResponse(projection, TransactionBasicResponse.class);
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
