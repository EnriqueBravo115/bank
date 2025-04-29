package dev.enrique.bank.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.enrique.bank.model.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // All Transactions where the Account is source or target
    @Query("""
            SELECT t FROM Transaction t 
            WHERE t.sourceAccount.id = :accountId
            OR t.targetAccount.id = :accountId
            """)
    List<Transaction> findAllByAccountId(@Param("accountId") Long accountId);

    // Only source transactions
    @Query("SELECT t FROM Transaction t WHERE t.sourceAccount.id = :accountId")
    List<Transaction> findOutgoingByAccountId(@Param("accountId") Long accountId);

    // Only source transactions
    @Query("SELECT t FROM Transaction t WHERE t.targetAccount.id = :accountId")
    List<Transaction> findIncomingByAccountId(@Param("accountId") Long accountId);
}
