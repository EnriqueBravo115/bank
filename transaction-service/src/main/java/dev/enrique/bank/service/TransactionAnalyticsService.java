package dev.enrique.bank.service;

import java.math.BigDecimal;
import java.time.Month;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import dev.enrique.bank.commons.dto.response.TransactionSummaryResponse;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.dao.projection.TransactionBasicProjection;
import dev.enrique.bank.dao.projection.TransactionDetailedProjection;

@Service
public interface TransactionAnalyticsService {
    Map<TransactionType, List<TransactionDetailedProjection>> groupTransactionsByType(String accountNumber,
            TransactionStatus transactionStatus);

    Map<TransactionType, BigDecimal> sumTransactionsByType(String accountNumber, TransactionStatus status);

    Map<Boolean, List<TransactionBasicProjection>> partitionTransactionsByAmount(String accountNumber,
            TransactionStatus status, BigDecimal amount);

    Map<TransactionType, TransactionSummaryResponse> getTransactionTypeSummary(String accountNumber,
            TransactionStatus status);

    BigDecimal calculateTotalAmountByStatusAndType(String accountNumber, TransactionStatus status,
            TransactionType type);

    double getAverageDaysBetweenTransactions(String accountNumber, TransactionStatus status);

    Map<TransactionType, List<TransactionBasicProjection>> getMaxTransactionByType(String accountNumber,
            TransactionStatus status);

    Map<Month, Long> countTransactionsByMonth(String accountNumber, TransactionStatus status);

    Map<TransactionType, BigDecimal> getAverageAmountByType(String accountNumber, TransactionStatus status);
}
