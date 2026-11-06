package dev.enrique.bank.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import dev.enrique.bank.commons.dto.response.TransactionResponse;
import dev.enrique.bank.commons.enums.IdentifierType;
import dev.enrique.bank.commons.enums.TransactionStatus;

public interface TransactionQueryService {
    List<TransactionResponse> getTransactionHistory(String sourceIdentifier, TransactionStatus status);

    Page<TransactionResponse> getAllTransactions(String sourceIdentifier, TransactionStatus status, Pageable pageable);

    List<TransactionResponse> getTransactionsByYear(String sourceIdentifier, TransactionStatus status, Integer year);

    List<TransactionResponse> getAllTransactionsBySourceIdentifiers(List<String> sourceIdentifiers);

    TransactionResponse getMaxTransaction(String sourceIdentifier, TransactionStatus status);

    List<TransactionResponse> getTransactionsInDateRange(String sourceIdentifier, TransactionStatus status,
            LocalDateTime startDate, LocalDateTime endDate);

    TransactionResponse getTransactionByCode(String transactionCode);

    List<TransactionResponse> getAllTransactionByAmountRangeAndStatus(String sourceIdentifier,
            TransactionStatus status, BigDecimal minAmount, BigDecimal maxAmount);

    List<TransactionResponse> getAllByIdentifierTypeAndStatus(String sourceIdentifier,
            IdentifierType identifierType, TransactionStatus status);

    List<TransactionResponse> getTransactionByKeyword(String sourceIdentifier, TransactionStatus status,
            String keyword);
}
