package dev.enrique.bank.service;

import java.math.BigDecimal;
import java.time.Month;
import java.util.List;
import java.util.Map;

import dev.enrique.bank.commons.dto.response.TransactionResponse;
import dev.enrique.bank.commons.dto.response.TransactionSummaryResponse;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;

public interface TransactionAnalyticsService {
    Map<TransactionType, List<TransactionResponse>> groupTransactionsByType(String sourceIdentifier,
            TransactionStatus status);

    Map<TransactionType, BigDecimal> sumTransactionsByType(String sourceIdentifier, TransactionStatus status);

    Map<Boolean, List<TransactionResponse>> partitionTransactionsByAmount(String sourceIdentifier,
            TransactionStatus status, BigDecimal amount);

    Map<TransactionType, TransactionSummaryResponse> getTransactionTypeSummary(String sourceIdentifier,
            TransactionStatus status);

    BigDecimal calculateTotalAmountByStatusAndType(String sourceIdentifier, TransactionStatus status,
            TransactionType type);

    double getAverageDaysBetweenTransactions(String sourceIdentifier, TransactionStatus status);

    Map<TransactionType, TransactionResponse> getMaxTransactionByType(String sourceIdentifier,
            TransactionStatus status);

    Map<Month, Long> countTransactionsByMonth(String sourceIdentifier, TransactionStatus status);

    Map<TransactionType, BigDecimal> getAverageAmountByType(String sourceIdentifier, TransactionStatus status);
}
