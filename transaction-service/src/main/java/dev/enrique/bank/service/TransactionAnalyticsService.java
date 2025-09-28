package dev.enrique.bank.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import dev.enrique.bank.commons.dto.response.TransactionCommonResponse;
import dev.enrique.bank.commons.dto.response.TransactionDetailedResponse;
import dev.enrique.bank.commons.dto.response.TransactionSummaryResponse;
import dev.enrique.bank.commons.enums.TransactionType;

@Service
public interface TransactionAnalyticsService {
    Map<TransactionType, List<TransactionDetailedResponse>> groupTransactionsByType(String accountNumber);
    Map<TransactionType, BigDecimal> sumTransactionsByType(String accountNumber);
    Map<Boolean, List<TransactionCommonResponse>> partitionTransactionsByAmount(String accountNumber,
            BigDecimal amount);
    Map<TransactionType, TransactionSummaryResponse> getTransactionTypeSummary(String accountNumber);
    BigDecimal calculateTotalTransactionAmount(String accountNumber);
    BigDecimal calculateTotalAmountByType(String accountNumber, TransactionType type);
    double getAverageDaysBetweenTransactions(String accountNumber);
}
