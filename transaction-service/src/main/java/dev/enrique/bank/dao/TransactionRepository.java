package dev.enrique.bank.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.dao.projection.TransactionCommonProjection;
import dev.enrique.bank.dao.projection.TransactionDetailedProjection;
import dev.enrique.bank.model.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Query("""
            SELECT t FROM Transaction t
            WHERE t.sourceIdentifier = :sourceIdentifier
            AND t.transactionStatus = :status
            ORDER BY t.transactionDate DESC
            """)
    <T> List<T> findAllBySourceIdentifierAndStatus(
            @Param("sourceIdentifier") String sourceIdentifier,
            @Param("status") TransactionStatus status,
            Class<T> type);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.sourceIdentifier = :sourceIdentifier
            AND t.transactionStatus = :status
            AND t.transactionType = :type
            ORDER BY t.transactionDate DESC
            """)
    <T> List<T> findAllBySourceIdentifierStatusAndType(
            @Param("sourceIdentifier") String sourceIdentifier,
            @Param("status") TransactionStatus status,
            @Param("type") TransactionType transactionType,
            Class<T> type);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.sourceIdentifier = :sourceIdentifier
            AND (t.transactionStatus = :status)
            ORDER BY t.transactionDate DESC
            """)
    Page<TransactionCommonProjection> findAllPageableBySourceIdentifierAndStatus(
            @Param("sourceIdentifier") String sourceIdentifier,
            @Param("status") TransactionStatus status,
            Pageable pageable);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.sourceIdentifier = :sourceIdentifier
            AND (t.transactionStatus = 'COMPLETED')
            """)
    <T> List<T> findAllCompletedBySourceIdentifier(
            @Param("sourceI") String sourceIdentifier,
            Class<T> type);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.sourceIdentifier = :sourceIdentifier
            AND (YEAR(t.transactionDate) = :year)
            AND t.transactionStatus = :status
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionDetailedProjection> findAllBySourceIdentifierAndYear(
            @Param("sourceIdentifier") String sourceIdentifier,
            @Param("year") Integer year,
            @Param("status") TransactionStatus status);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.sourceIdentifier IN :sourceIdentifiers
            AND (t.transactionStatus = 'COMPLETED')
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionCommonProjection> findAllCompletedBySourceIdentifiersIn(
            @Param("sourceIdentifiers") List<String> sourceIdentifiers);
}
