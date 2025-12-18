package dev.enrique.bank.service;

import dev.enrique.bank.commons.enums.IdentifierType;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.dao.projection.TransactionCommonProjection;
import dev.enrique.bank.dao.projection.TransactionDetailedProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionQueryService {
    List<TransactionDetailedProjection> getTransactionHistory(String sourceIdentifier, TransactionStatus status);

    Page<TransactionCommonProjection> getAllTransactions(String sourceIdentifier, TransactionStatus status,
                                                         Pageable pageable);

    List<TransactionDetailedProjection> getTransactionsByYear(String sourceIdentifier, TransactionStatus status, Integer year);

    List<TransactionCommonProjection> getAllTransactionsBySourceIdentifiers(List<String> sourceIdentifiers);

    Optional<TransactionCommonProjection> getMaxTransaction(String sourceIdentifier, TransactionStatus status);

    List<TransactionCommonProjection> getTransactionsInDateRange(String sourceIdentifier, TransactionStatus status,
                                                                 LocalDateTime startDate, LocalDateTime endDate);

    Optional<TransactionDetailedProjection> getTransactionByCode(String transactionCode);

    List<TransactionCommonProjection> getAllTransactionByAmountRangeAndStatus(String sourceIdentifier, TransactionStatus status,
                                                                              BigDecimal minAmount, BigDecimal maxAmount);

    List<TransactionCommonProjection> getAllByIdentifierTypeAndStatus(String sourceIdentifier, IdentifierType identifierType,
                                                                      TransactionStatus status);

    List<TransactionDetailedProjection> getTransactionByKeyword(String sourceIdentifier, TransactionStatus status,
                                                                String keyword);
}
