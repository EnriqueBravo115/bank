package dev.enrique.bank.service;

import java.math.BigDecimal;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.dao.projection.TransactionBasicProjection;
import dev.enrique.bank.dao.projection.TransactionCommonProjection;
import dev.enrique.bank.dao.projection.TransactionDetailedProjection;
import dev.enrique.bank.model.Transaction;

public interface TransactionService {
    List<TransactionDetailedProjection> getTransactionHistory(Long accountId);

    Page<TransactionCommonProjection> getAllTransactions(Long accountId, Pageable pageable);

    List<TransactionCommonProjection> getTransactionByYearAndAccount(Long accountId, Integer year);

    List<TransactionBasicProjection> getAllTransactionsReversals(Long accountId);

    List<TransactionCommonProjection> getAllTransactionsFromAccounts(List<Long> accountIds);

    Map<TransactionType, List<TransactionCommonProjection>> groupTransactionsByType(Long accountId);

    Map<TransactionType, BigDecimal> sumTransactionsByType(Long accountId);

    IntSummaryStatistics getTransactionYearStatistics(Long accountId);

    Map<Boolean, List<TransactionBasicProjection>> partitionTransactionsByAmount(Long accountId, BigDecimal amount);

    Map<TransactionType, String> getTransactionTypeSummary(Long accountId);

    BigDecimal calculateTotalTransactionAmount(Long accountId);

    BigDecimal calculateTotalAmountByType(Long accountId, TransactionType type);

    BigDecimal calculateTransferFee(BigDecimal amount, Currency currency);

    Boolean hasSufficientFunds(Long accountId, BigDecimal amount);

    BigDecimal getTransferLimit(Long accountId);

    Set<String> getAllUniqueTransactionDescriptions(Long accountId);

    String getAllTransactionDescriptions(Long accountId);

    String getFormattedAverageBalance(List<Long> accountIds);

    Optional<TransactionBasicProjection> findMaxTransaction(Long accountId);

    double getAverageDaysBetweenTransactions(Long accountId);
}
