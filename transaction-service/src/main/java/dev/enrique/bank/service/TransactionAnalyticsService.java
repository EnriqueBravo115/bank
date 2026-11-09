package dev.enrique.bank.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import dev.enrique.bank.commons.dto.response.TransactionResponse;
import dev.enrique.bank.commons.dto.response.TransactionSummaryResponse;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;

public interface TransactionAnalyticsService {
    Map<TransactionType, List<TransactionResponse>> groupTransactionsByType(String accountNumber,
            TransactionStatus status);

    Map<TransactionType, BigDecimal> sumTransactionsByType(String accountNumber, TransactionStatus status);

    Map<Boolean, List<TransactionResponse>> partitionTransactionsByAmount(String accountNumber,
            TransactionStatus status, BigDecimal amount);

    Map<TransactionType, TransactionSummaryResponse> getTransactionTypeSummary(String accountNumber,
            TransactionStatus status);

    BigDecimal calculateTotalAmountByStatusAndType(String accountNumber, TransactionStatus status,
            TransactionType type);

    double getAverageDaysBetweenTransactions(String accountNumber, TransactionStatus status);

    Map<TransactionType, TransactionResponse> getMaxTransactionByType(String accountNumber,
            TransactionStatus status);

    Map<YearMonth, Long> countTransactionsByMonth(String accountNumber, TransactionStatus status);

    Map<TransactionType, BigDecimal> getAverageAmountByType(String accountNumber, TransactionStatus status);
}
