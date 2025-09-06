package dev.enrique.bank.service;

import java.math.BigDecimal;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Pageable;

import dev.enrique.bank.dao.projection.TransactionBasicProjection;
import dev.enrique.bank.dao.projection.TransactionCommonProjection;
import dev.enrique.bank.dto.response.TransactionBasicResponse;
import dev.enrique.bank.dto.response.TransactionCommonResponse;
import dev.enrique.bank.dto.response.TransactionDetailedResponse;
import dev.enrique.bank.enums.TransactionType;
import dev.enrique.bank.mapper.HeaderResponse;

public interface TransactionService {

    List<TransactionDetailedResponse> getTransactionHistory(Long accountId);

    HeaderResponse<TransactionCommonResponse> getAllTransactions(Long accountId, Pageable pageable);

    List<TransactionCommonResponse> getTransactionByYearAndAccount(Long accountId, Integer year);

    List<TransactionBasicResponse> getAllTransactionsReversals(Long accountId);

    List<TransactionCommonResponse> getAllTransactionsFromAccounts(List<Long> accountIds);

    Map<TransactionType, List<TransactionCommonResponse>> groupTransactionsByType(Long accountId);

    Map<TransactionType, BigDecimal> sumTransactionsByType(Long accountId);

    IntSummaryStatistics getTransactionYearStatistics(Long accountId);

    Map<Boolean, List<TransactionBasicResponse>> partitionTransactionsByAmount(Long accountId, BigDecimal amount);

    Map<TransactionType, String> getTransactionTypeSummary(Long accountId);

    BigDecimal calculateTotalTransactionAmount(Long accountId);

    BigDecimal calculateTotalAmountByType(Long accountId, TransactionType type);

    Boolean hasSufficientFunds(Long accountId, BigDecimal amount);

    Set<String> getAllUniqueTransactionDescriptions(Long accountId);

    String getAllTransactionDescriptions(Long accountId);

    String getFormattedAverageBalance(List<Long> accountIds);

    Optional<TransactionBasicResponse> findMaxTransaction(Long accountId);

    double getAverageDaysBetweenTransactions(Long accountId);
}
