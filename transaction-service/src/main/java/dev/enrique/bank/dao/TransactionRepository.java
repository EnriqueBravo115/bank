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

/**
 * Two kinds of queries by account:
 * <ul>
 * <li>visible: what the account sent plus the COMPLETED transfers it received (counterparty), each with
 * its direction as seen from the account. History, search and lookups use these.</li>
 * <li>own: only the operations the account made (and its deposits). Analytics and support use these, so
 * totals never add incoming and outgoing money together.</li>
 * </ul>
 * ownerId is null for admins; otherwise the caller must own the side of the transaction they look from.
 */
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    // credit: a deposit into the account, or money received from another account
    String SELECT_RESPONSE = """
            SELECT new dev.enrique.bank.commons.dto.response.TransactionResponse(
                t.transactionCode, t.amount, t.description, t.transactionDate,
                t.transactionType, t.transactionStatus,
                CASE WHEN t.transactionType = 'DEPOSIT' OR t.accountNumber <> :accountNumber THEN true
                    ELSE false END)
            FROM Transaction t
            """;

    String VISIBLE = """
            ((t.accountNumber = :accountNumber AND (:ownerId IS NULL OR t.ownerId = :ownerId))
                OR (t.counterpartyAccountNumber = :accountNumber AND t.transactionStatus = 'COMPLETED'
                    AND (:ownerId IS NULL OR t.counterpartyOwnerId = :ownerId)))
            """;

    String OWN = """
            t.accountNumber = :accountNumber AND (:ownerId IS NULL OR t.ownerId = :ownerId)
            """;

    @Query(SELECT_RESPONSE + "WHERE " + VISIBLE + """
            AND t.transactionStatus = :status
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionResponse> findVisibleByAccountNumberAndStatus(String ownerId, String accountNumber,
            TransactionStatus status);

    @Query(value = SELECT_RESPONSE + "WHERE " + VISIBLE + """
            AND t.transactionStatus = :status
            ORDER BY t.transactionDate DESC
            """,
            countQuery = "SELECT COUNT(t) FROM Transaction t WHERE " + VISIBLE + """
            AND t.transactionStatus = :status
            """)
    Page<TransactionResponse> findVisiblePageByAccountNumberAndStatus(
            String ownerId, String accountNumber, TransactionStatus status, Pageable pageable);

    @Query(SELECT_RESPONSE + "WHERE " + VISIBLE + """
            AND YEAR(t.transactionDate) = :year
            AND t.transactionStatus = :status
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionResponse> findVisibleByAccountNumberAndYear(
            String ownerId, String accountNumber, Integer year, TransactionStatus status);

    @Query(SELECT_RESPONSE + "WHERE " + VISIBLE + """
            AND t.transactionStatus = :status
            AND t.transactionDate BETWEEN :startDate AND :endDate
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionResponse> findVisibleByAccountNumberAndDateRangeAndStatus(
            String ownerId, String accountNumber, TransactionStatus status, LocalDateTime startDate,
            LocalDateTime endDate);

    @Query(SELECT_RESPONSE + "WHERE " + VISIBLE + """
            AND t.transactionStatus = :status
            AND t.amount BETWEEN :minAmount AND :maxAmount
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionResponse> findVisibleByAmountRangeAndStatus(
            String ownerId, String accountNumber, TransactionStatus status, BigDecimal minAmount,
            BigDecimal maxAmount);

    // keyword arrives with % and _ escaped by '\\' (see TransactionQueryServiceImpl)
    @Query(SELECT_RESPONSE + "WHERE " + VISIBLE + """
            AND t.transactionStatus = :status
            AND (LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\'
                OR LOWER(t.reason) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\')
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionResponse> searchVisibleByKeyword(String ownerId, String accountNumber, TransactionStatus status,
            String keyword);

    @Query(SELECT_RESPONSE + "WHERE " + VISIBLE + """
            AND t.transactionStatus = :status
            ORDER BY t.amount DESC
            LIMIT 1
            """)
    Optional<TransactionResponse> findVisibleMaxByAccountNumberAndStatus(String ownerId, String accountNumber,
            TransactionStatus status);

    // Admin report over several accounts: money moved between two of them shows up once, as DEBIT.
    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.TransactionResponse(
                t.transactionCode, t.amount, t.description, t.transactionDate,
                t.transactionType, t.transactionStatus,
                CASE WHEN t.transactionType = 'DEPOSIT' OR t.accountNumber NOT IN :accountNumbers THEN true
                    ELSE false END)
            FROM Transaction t
            WHERE (t.accountNumber IN :accountNumbers OR t.counterpartyAccountNumber IN :accountNumbers)
            AND t.transactionStatus = 'COMPLETED'
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionResponse> findAllCompletedByAccountNumberIn(List<String> accountNumbers);

    // The sender sees it as DEBIT; the receiver of a COMPLETED transfer, as CREDIT.
    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.TransactionResponse(
                t.transactionCode, t.amount, t.description, t.transactionDate,
                t.transactionType, t.transactionStatus,
                CASE WHEN t.transactionType = 'DEPOSIT' THEN true
                    WHEN :ownerId IS NOT NULL AND t.ownerId <> :ownerId THEN true
                    ELSE false END)
            FROM Transaction t
            WHERE t.transactionCode = :transactionCode
            AND (:ownerId IS NULL OR t.ownerId = :ownerId
                OR (t.counterpartyOwnerId = :ownerId AND t.transactionStatus = 'COMPLETED'))
            """)
    Optional<TransactionResponse> findResponseByTransactionCode(String ownerId, String transactionCode);

    // identifierType is how the account itself was referenced when it made the transaction
    @Query(SELECT_RESPONSE + "WHERE " + OWN + """
            AND t.identifierType = :identifierType
            AND t.transactionStatus = :status
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionResponse> findOwnByIdentifierTypeAndStatus(
            String ownerId, String accountNumber, IdentifierType identifierType, TransactionStatus status);

    @Query(SELECT_RESPONSE + "WHERE " + OWN + """
            AND t.transactionStatus = :status
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionResponse> findOwnByAccountNumberAndStatus(String ownerId, String accountNumber,
            TransactionStatus status);

    @Query(SELECT_RESPONSE + "WHERE " + OWN + """
            AND t.transactionStatus = :status
            AND t.transactionType = :type
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionResponse> findOwnByAccountNumberStatusAndType(
            String ownerId, String accountNumber, TransactionStatus status, TransactionType type);

    @Query(SELECT_RESPONSE + "WHERE " + OWN + """
            AND t.transactionStatus = 'COMPLETED'
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionResponse> findOwnCompletedByAccountNumber(String ownerId, String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.transactionCode = :transactionCode")
    Optional<Transaction> lockByTransactionCode(String transactionCode);

    // PENDING transactions due for another attempt; rows locked by another replica (or by a
    // cancellation in progress) are skipped. Stuck ones (next_attempt_at NULL) wait for an admin.
    @Query(value = """
            SELECT * FROM transactions
            WHERE transaction_status = 'PENDING'
            AND next_attempt_at <= :now
            ORDER BY next_attempt_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Transaction> lockPending(LocalDateTime now, int limit);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.transactionStatus = 'PENDING' AND t.nextAttemptAt IS NULL
            ORDER BY t.transactionDate
            """)
    List<Transaction> findStuck();
}
