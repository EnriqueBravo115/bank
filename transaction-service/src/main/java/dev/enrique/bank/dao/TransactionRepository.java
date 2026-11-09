package dev.enrique.bank.dao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import dev.enrique.bank.commons.dto.response.TransactionResponse;
import dev.enrique.bank.commons.enums.IdentifierType;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.model.Transaction;
import jakarta.persistence.LockModeType;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    String SELECT_RESPONSE = """
            SELECT new dev.enrique.bank.commons.dto.response.TransactionResponse(
                t.transactionCode, t.amount, t.description, t.transactionDate,
                t.transactionType, t.transactionStatus)
            FROM Transaction t
            """;

    @Query(SELECT_RESPONSE + """
            WHERE t.accountNumber = :accountNumber
            AND (:ownerId IS NULL OR t.ownerId = :ownerId)
            AND t.transactionStatus = :status
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionResponse> findAllByAccountNumberAndStatus(String ownerId, String accountNumber, TransactionStatus status);

    @Query(SELECT_RESPONSE + """
            WHERE t.accountNumber = :accountNumber
            AND (:ownerId IS NULL OR t.ownerId = :ownerId)
            AND t.transactionStatus = :status
            AND t.transactionType = :type
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionResponse> findAllByAccountNumberStatusAndType(
            String ownerId, String accountNumber, TransactionStatus status, TransactionType type);

    @Query(value = SELECT_RESPONSE + """
            WHERE t.accountNumber = :accountNumber
            AND (:ownerId IS NULL OR t.ownerId = :ownerId)
            AND t.transactionStatus = :status
            ORDER BY t.transactionDate DESC
            """,
            countQuery = """
            SELECT COUNT(t) FROM Transaction t
            WHERE t.accountNumber = :accountNumber
            AND (:ownerId IS NULL OR t.ownerId = :ownerId)
            AND t.transactionStatus = :status
            """)
    Page<TransactionResponse> findPageByAccountNumberAndStatus(
            String ownerId, String accountNumber, TransactionStatus status, Pageable pageable);

    @Query(SELECT_RESPONSE + """
            WHERE t.accountNumber = :accountNumber
            AND (:ownerId IS NULL OR t.ownerId = :ownerId)
            AND t.transactionStatus = 'COMPLETED'
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionResponse> findAllCompletedByAccountNumber(String ownerId, String accountNumber);

    @Query(SELECT_RESPONSE + """
            WHERE t.accountNumber IN :accountNumbers
            AND t.transactionStatus = 'COMPLETED'
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionResponse> findAllCompletedByAccountNumberIn(List<String> accountNumbers);

    @Query(SELECT_RESPONSE + """
            WHERE t.accountNumber = :accountNumber
            AND (:ownerId IS NULL OR t.ownerId = :ownerId)
            AND YEAR(t.transactionDate) = :year
            AND t.transactionStatus = :status
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionResponse> findAllByAccountNumberAndYear(
            String ownerId, String accountNumber, Integer year, TransactionStatus status);

    @Query(SELECT_RESPONSE + """
            WHERE t.accountNumber = :accountNumber
            AND (:ownerId IS NULL OR t.ownerId = :ownerId)
            AND t.transactionStatus = :status
            AND t.transactionDate BETWEEN :startDate AND :endDate
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionResponse> findAllByAccountNumberAndDateRangeAndStatus(
            String ownerId, String accountNumber, TransactionStatus status, LocalDateTime startDate, LocalDateTime endDate);

    @Query(SELECT_RESPONSE + """
            WHERE t.accountNumber = :accountNumber
            AND (:ownerId IS NULL OR t.ownerId = :ownerId)
            AND t.transactionStatus = :status
            AND t.amount BETWEEN :minAmount AND :maxAmount
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionResponse> findAllByAmountRangeAndStatus(
            String ownerId, String accountNumber, TransactionStatus status, BigDecimal minAmount, BigDecimal maxAmount);

    @Query(SELECT_RESPONSE + """
            WHERE t.accountNumber = :accountNumber
            AND (:ownerId IS NULL OR t.ownerId = :ownerId)
            AND t.identifierType = :identifierType
            AND t.transactionStatus = :status
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionResponse> findAllByIdentifierTypeAndStatus(
            String ownerId, String accountNumber, IdentifierType identifierType, TransactionStatus status);

    @Query(SELECT_RESPONSE + """
            WHERE t.accountNumber = :accountNumber
            AND (:ownerId IS NULL OR t.ownerId = :ownerId)
            AND t.transactionStatus = :status
            AND (LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(t.reason) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionResponse> searchByKeyword(String ownerId, String accountNumber, TransactionStatus status, String keyword);

    @Query(SELECT_RESPONSE + """
            WHERE t.transactionCode = :transactionCode
            AND (:ownerId IS NULL OR t.ownerId = :ownerId)
            """)
    Optional<TransactionResponse> findResponseByTransactionCode(String ownerId, String transactionCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.transactionCode = :transactionCode")
    Optional<Transaction> lockByTransactionCode(String transactionCode);

    // PENDING transactions due for another attempt; rows locked by another replica (or by a
    // cancellation in progress) are skipped.
    @Query(value = """
            SELECT * FROM transactions
            WHERE transaction_status = 'PENDING'
            AND next_attempt_at <= :now
            ORDER BY next_attempt_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Transaction> lockPending(LocalDateTime now, int limit);

    @Query(SELECT_RESPONSE + """
            WHERE t.accountNumber = :accountNumber
            AND (:ownerId IS NULL OR t.ownerId = :ownerId)
            AND t.transactionStatus = :status
            ORDER BY t.amount DESC
            LIMIT 1
            """)
    Optional<TransactionResponse> findMaxByAccountNumberAndStatus(String ownerId, String accountNumber, TransactionStatus status);
}
