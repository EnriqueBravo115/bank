package dev.enrique.bank.service;

import java.math.BigDecimal;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import dev.enrique.bank.dto.response.TransactionBasicResponse;
import dev.enrique.bank.dto.response.TransactionCommonResponse;
import dev.enrique.bank.enums.TransactionType;

@Service
public interface TransactionAnalyticsService {
    Map<TransactionType, List<TransactionCommonResponse>> groupTransactionsByType(Long accountId);

    Map<TransactionType, BigDecimal> sumTransactionsByType(Long accountId);

    IntSummaryStatistics getTransactionYearStatistics(Long accountId);

    Map<Boolean, List<TransactionBasicResponse>> partitionTransactionsByAmount(Long accountId, BigDecimal amount);

    Map<TransactionType, String> getTransactionTypeSummary(Long accountId);

    BigDecimal calculateTotalTransactionAmount(Long accountId);

    BigDecimal calculateTotalAmountByType(Long accountId, TransactionType type);

    double getAverageDaysBetweenTransactions(Long accountId);
}
