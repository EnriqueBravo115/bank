package dev.enrique.bank.service.impl;

import dev.enrique.bank.commons.enums.IdentifierType;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.dao.projection.TransactionCommonProjection;
import dev.enrique.bank.dao.projection.TransactionDetailedProjection;
import dev.enrique.bank.service.TransactionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    public Page<TransactionCommonProjection> getAllTransactions(
            String sourceIdentifier, TransactionStatus status, Pageable pageable) {
        return transactionRepository
                .findAllPageableBySourceIdentifierAndStatus(sourceIdentifier, status, pageable);
    }

    @Override
    public List<TransactionDetailedProjection> getTransactionsByYear(
            String sourceIdentifier, TransactionStatus status, Integer year) {
        return transactionRepository
                .findAllBySourceIdentifierAndStatus(sourceIdentifier, status, TransactionDetailedProjection.class)
                .stream()
                .filter(n -> n.getTransactionDate().getYear() == year)
                .toList();
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

    @Override
    public List<TransactionCommonProjection> getTransactionsInDateRange(
            String sourceIdentifier, TransactionStatus status, LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository
                .findAllBySourceIdentifierAndDateRangeAndStatus(sourceIdentifier, status, startDate, endDate);
    }

    @Override
    public Optional<TransactionDetailedProjection> getTransactionByCode(String transactionCode) {
        return transactionRepository.findByTransactionCode(transactionCode);
    }

    @Override
    public List<TransactionCommonProjection> getAllTransactionByAmountRangeAndStatus(
            String sourceIdentifier, TransactionStatus status, BigDecimal minAmount, BigDecimal maxAmount) {
        return transactionRepository.findAllByAmountRangeAndStatus(sourceIdentifier, status, minAmount, maxAmount);
    }

    @Override
    public List<TransactionCommonProjection> getAllByIdentifierTypeAndStatus(
            String sourceIdentifier, IdentifierType identifierType, TransactionStatus status) {
        return transactionRepository.findAllByIdentifierTypeAndStatus(sourceIdentifier, identifierType, status);
    }

    @Override
    public List<TransactionDetailedProjection> getTransactionByKeyword(
            String sourceIdentifier, TransactionStatus status, String keyword) {
        return transactionRepository.searchByKeyword(sourceIdentifier, status, keyword);
    }
}
