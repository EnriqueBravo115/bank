package dev.enrique.bank.service;

import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.dao.projection.TransactionCommonProjection;
import dev.enrique.bank.dao.projection.TransactionDetailedProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface TransactionQueryService {
    List<TransactionDetailedProjection> getTransactionHistory(String sourceIdentifier, TransactionStatus status);

    Page<TransactionCommonProjection> getAllTransactions(String sourceIdentifier, TransactionStatus status,
                                                         Pageable pageable);

    List<TransactionDetailedProjection> getTransactionsByYear(String sourceIdentifier, TransactionStatus status, Integer year);

    List<TransactionCommonProjection> getAllTransactionsBySourceIdentifiers(List<String> sourceIdentifiers);

    Optional<TransactionCommonProjection> getMaxTransaction(String sourceIdentifier, TransactionStatus status);
}
