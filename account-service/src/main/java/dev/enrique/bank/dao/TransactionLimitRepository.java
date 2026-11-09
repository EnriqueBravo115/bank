package dev.enrique.bank.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import dev.enrique.bank.commons.enums.LimitType;
import dev.enrique.bank.commons.enums.TimePeriod;
import dev.enrique.bank.model.TransactionLimit;

// At most one limit per account, type and period (unique constraint).
public interface TransactionLimitRepository extends JpaRepository<TransactionLimit, Long> {
    @Query("""
            SELECT tl FROM TransactionLimit tl JOIN FETCH tl.account
            WHERE tl.account.accountNumber = :accountNumber
            ORDER BY tl.limitType, tl.timePeriod
            """)
    List<TransactionLimit> findAllByAccountNumber(String accountNumber);

    @Query("""
            SELECT tl FROM TransactionLimit tl JOIN FETCH tl.account
            WHERE tl.account.userId = :userId
            ORDER BY tl.limitType, tl.timePeriod
            """)
    List<TransactionLimit> findAllByUserId(String userId);

    @Query("""
            SELECT tl FROM TransactionLimit tl
            WHERE tl.account.id = :accountId AND tl.limitType = :limitType
            """)
    List<TransactionLimit> findAllByAccountIdAndLimitType(Long accountId, LimitType limitType);

    Optional<TransactionLimit> findByAccountIdAndLimitTypeAndTimePeriod(
            Long accountId, LimitType limitType, TimePeriod timePeriod);
}
