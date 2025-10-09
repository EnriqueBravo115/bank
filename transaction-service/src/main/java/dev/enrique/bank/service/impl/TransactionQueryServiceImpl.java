package dev.enrique.bank.service.impl;

import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.dao.projection.TransactionCommonProjection;
import dev.enrique.bank.dao.projection.TransactionDetailedProjection;
import dev.enrique.bank.service.TransactionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionQueryServiceImpl implements TransactionQueryService {
    private final TransactionRepository transactionRepository;

    @Override
    public List<TransactionDetailedProjection> getTransactionHistory(String sourceIdentifier, TransactionStatus status) {
        return transactionRepository
                .findAllBySourceIdentifierAndStatus(sourceIdentifier, status, TransactionDetailedProjection.class);
    }

    @Override
    public Page<TransactionCommonProjection> getAllTransactions(String sourceIdentifier, TransactionStatus status,
                                                                Pageable pageable) {
        return transactionRepository
                .findAllPageableBySourceIdentifierAndStatus(sourceIdentifier, status, pageable);
    }

    @Override
    public List<TransactionDetailedProjection> getTransactionsByYear(String sourceIdentifier, TransactionStatus status,
                                                                     Integer year) {
        return transactionRepository
                .findAllBySourceIdentifierAndYear(sourceIdentifier, year, status);
    }

    // This function must be executed with "ADMIN" authorities
    @Override
    public List<TransactionCommonProjection> getAllTransactionsBySourceIdentifiers(List<String> sourceIdentifiers) {
        return transactionRepository
                .findAllCompletedBySourceIdentifiersIn(sourceIdentifiers);
    }

    @Override
    public Optional<TransactionCommonProjection> getMaxTransaction(String sourceIdentifier, TransactionStatus status) {
        return transactionRepository
                .findAllCompletedBySourceIdentifier(sourceIdentifier, TransactionCommonProjection.class)
                .stream()
                .reduce((t1, t2) -> t1.getAmount().compareTo(t2.getAmount()) > 0 ? t1 : t2);
    }
}
