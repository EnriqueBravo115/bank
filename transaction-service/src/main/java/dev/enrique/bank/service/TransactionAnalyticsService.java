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
    Map<TransactionType, List<TransactionDetailedProjection>> groupTransactionsByType(String sourceIdentifier,
            TransactionStatus transactionStatus);

    Map<TransactionType, BigDecimal> sumTransactionsByType(String sourceIdentifier, TransactionStatus status);

    Map<Boolean, List<TransactionBasicProjection>> partitionTransactionsByAmount(String sourceIdentifier,
            TransactionStatus status, BigDecimal amount);

    Map<TransactionType, TransactionSummaryResponse> getTransactionTypeSummary(String sourceIdentifier,
            TransactionStatus status);

    BigDecimal calculateTotalAmountByStatusAndType(String sourceIdentifier, TransactionStatus status,
            TransactionType type);

    double getAverageDaysBetweenTransactions(String sourceIdentifier, TransactionStatus status);

    Map<TransactionType, List<TransactionBasicProjection>> getMaxTransactionByType(String sourceIdentifier,
            TransactionStatus status);

    Map<Month, Long> countTransactionsByMonth(String sourceIdentifier, TransactionStatus status);

    Map<TransactionType, BigDecimal> getAverageAmountByType(String sourceIdentifier, TransactionStatus status);
}
