package dev.enrique.bank.dao;

import dev.enrique.bank.commons.enums.IdentifierType;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.dao.projection.TransactionCommonProjection;
import dev.enrique.bank.dao.projection.TransactionDetailedProjection;
import dev.enrique.bank.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    <T> List<T> findAllCompletedBySourceIdentifier(@Param("sourceIdentifier") String sourceIdentifier, Class<T> type);

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
            WHERE t.sourceIdentifier = :sourceIdentifier
            AND t.transactionDate BETWEEN :startDate AND :endDate
            """)
    List<TransactionCommonProjection> findAllBySourceIdentifierAndDateRangeAndStatus(
            @Param("sourceIdentifier") String sourceIdentifier,
            @Param("status") TransactionStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
                SELECT t FROM Transaction t
                WHERE t.sourceIdentifier = :sourceIdentifier
                AND t.transactionStatus = :status
                AND t.amount BETWEEN :minAmount AND :maxAmount
            """)
    List<TransactionCommonProjection> findAllByAmountRangeAndStatus(
            @Param("sourceIdentifier") String sourceIdentifier,
            @Param("status") TransactionStatus status,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount);


    @Query("""
            SELECT t FROM Transaction t
            WHERE t.sourceIdentifier IN :sourceIdentifiers
            AND (t.transactionStatus = 'COMPLETED')
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionCommonProjection> findAllCompletedBySourceIdentifiersIn(
            @Param("sourceIdentifiers") List<String> sourceIdentifiers);

    @Query("""
                SELECT t FROM Transaction t
                WHERE t.sourceIdentifier = :sourceIdentifier
                AND t.identifierType = :identifierType
                AND t.transactionStatus = :status
            """)
    List<TransactionCommonProjection> findAllByIdentifierTypeAndStatus(
            @Param("sourceIdentifier") String sourceIdentifier,
            @Param("identifierType") IdentifierType identifierType,
            @Param("status") TransactionStatus status);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.sourceIdentifier = :sourceIdentifier
            AND t.transactionStatus = :transactionStatus
            AND (LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(t.reason) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    List<TransactionDetailedProjection> searchByKeyword(
            @Param("sourceIdentifier") String sourceIdentifier,
            @Param("transactionStatus") TransactionStatus status,
            @Param("keyword") String keyword);

    Optional<TransactionDetailedProjection> findByTransactionCode(String transactionCode);
}
