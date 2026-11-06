package dev.enrique.bank.dao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import dev.enrique.bank.commons.dto.response.TransactionResponse;
import dev.enrique.bank.commons.enums.IdentifierType;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.model.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    String SELECT_RESPONSE = """
            SELECT new dev.enrique.bank.commons.dto.response.TransactionResponse(
                t.transactionCode, t.amount, t.description, t.transactionDate,
                t.transactionType, t.transactionStatus)
            FROM Transaction t
            """;

    @Query(SELECT_RESPONSE + """
            WHERE t.sourceIdentifier = :sourceIdentifier
            AND t.transactionStatus = :status
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionResponse> findAllBySourceIdentifierAndStatus(String sourceIdentifier, TransactionStatus status);

    @Query(SELECT_RESPONSE + """
            WHERE t.sourceIdentifier = :sourceIdentifier
            AND t.transactionStatus = :status
            AND t.transactionType = :type
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionResponse> findAllBySourceIdentifierStatusAndType(
            String sourceIdentifier, TransactionStatus status, TransactionType type);

    @Query(value = SELECT_RESPONSE + """
            WHERE t.sourceIdentifier = :sourceIdentifier
            AND t.transactionStatus = :status
            ORDER BY t.transactionDate DESC
            """,
            countQuery = """
            SELECT COUNT(t) FROM Transaction t
            WHERE t.sourceIdentifier = :sourceIdentifier
            AND t.transactionStatus = :status
            """)
    Page<TransactionResponse> findPageBySourceIdentifierAndStatus(
            String sourceIdentifier, TransactionStatus status, Pageable pageable);

    @Query(SELECT_RESPONSE + """
            WHERE t.sourceIdentifier = :sourceIdentifier
            AND t.transactionStatus = 'COMPLETED'
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionResponse> findAllCompletedBySourceIdentifier(String sourceIdentifier);

    @Query(SELECT_RESPONSE + """
            WHERE t.sourceIdentifier IN :sourceIdentifiers
            AND t.transactionStatus = 'COMPLETED'
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionResponse> findAllCompletedBySourceIdentifiersIn(List<String> sourceIdentifiers);

    @Query(SELECT_RESPONSE + """
            WHERE t.sourceIdentifier = :sourceIdentifier
            AND YEAR(t.transactionDate) = :year
            AND t.transactionStatus = :status
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionResponse> findAllBySourceIdentifierAndYear(
            String sourceIdentifier, Integer year, TransactionStatus status);

    @Query(SELECT_RESPONSE + """
            WHERE t.sourceIdentifier = :sourceIdentifier
            AND t.transactionStatus = :status
            AND t.transactionDate BETWEEN :startDate AND :endDate
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionResponse> findAllBySourceIdentifierAndDateRangeAndStatus(
            String sourceIdentifier, TransactionStatus status, LocalDateTime startDate, LocalDateTime endDate);

    @Query(SELECT_RESPONSE + """
            WHERE t.sourceIdentifier = :sourceIdentifier
            AND t.transactionStatus = :status
            AND t.amount BETWEEN :minAmount AND :maxAmount
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionResponse> findAllByAmountRangeAndStatus(
            String sourceIdentifier, TransactionStatus status, BigDecimal minAmount, BigDecimal maxAmount);

    @Query(SELECT_RESPONSE + """
            WHERE t.sourceIdentifier = :sourceIdentifier
            AND t.identifierType = :identifierType
            AND t.transactionStatus = :status
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionResponse> findAllByIdentifierTypeAndStatus(
            String sourceIdentifier, IdentifierType identifierType, TransactionStatus status);

    @Query(SELECT_RESPONSE + """
            WHERE t.sourceIdentifier = :sourceIdentifier
            AND t.transactionStatus = :status
            AND (LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(t.reason) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionResponse> searchByKeyword(String sourceIdentifier, TransactionStatus status, String keyword);

    @Query(SELECT_RESPONSE + "WHERE t.transactionCode = :transactionCode")
    Optional<TransactionResponse> findResponseByTransactionCode(String transactionCode);

    @Query(SELECT_RESPONSE + """
            WHERE t.sourceIdentifier = :sourceIdentifier
            AND t.transactionStatus = :status
            ORDER BY t.amount DESC
            LIMIT 1
            """)
    Optional<TransactionResponse> findMaxBySourceIdentifierAndStatus(String sourceIdentifier, TransactionStatus status);
}
