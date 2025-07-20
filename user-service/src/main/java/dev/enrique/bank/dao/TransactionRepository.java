package dev.enrique.bank.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.enrique.bank.dao.projection.TransactionBasicProjection;
import dev.enrique.bank.dao.projection.TransactionCommonProjection;
import dev.enrique.bank.model.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Query("""
            SELECT t FROM Transaction t
            WHERE (t.sourceAccount.id = :accountId OR t.targetAccount.id = :accountId)
            AND t.transactionStatus = 'COMPLETED'
            """)
    List<Transaction> findAllByAccountId(@Param("accountId") Long accountId);

    @Query("""
            SELECT t FROM Transaction t
            WHERE (t.sourceAccount.id IN :accountIds OR t.targetAccount.id IN :accountIds)
            AND t.transactionStatus = 'COMPLETED'
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionCommonProjection> findCompletedByAccountIdsIn(@Param("accountIds") List<Long> accountIds);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.sourceAccount.id = :accountId OR t.targetAccount.id = :accountId
            AND (t.transactionStatus = 'COMPLETED')
            ORDER BY t.transactionDate DESC
            """)
    <T> List<T> findCompletedByAccountId(@Param("accountId") Long accountId, Class<T> type);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.transactionStatus = 'COMPLETED'
            AND (t.sourceAccount.id = :accountId OR t.targetAccount.id = :accountId)
            ORDER BY t.transactionDate DESC
            """)
    Page<TransactionCommonProjection> findCompletedByAccountId(@Param("accountId") Long accountId, Pageable pageable);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.transactionType = 'REVERSAL'
            AND (t.sourceAccount.id = :accountId OR t.targetAccount.id = :accountId)
            """)
    List<TransactionBasicProjection> findReversalsByAccountId(@Param("accountId") Long accountId);

    @Query("""
            SELECT t FROM Transaction t
            WHERE (:year IS NULL OR YEAR(t.transactionDate) = :year)
            AND (:accountId IS NULL OR t.sourceAccount.id = :accountId OR t.targetAccount.id = :accountId)
            ORDER BY t.transactionDate DESC
            """)
    List<TransactionCommonProjection> findByAccountIdAndYear(
            @Param("accountId") Long accountId, @Param("year") Integer year);
}
