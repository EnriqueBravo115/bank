package dev.enrique.bank.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.enrique.bank.commons.enums.LimitType;
import dev.enrique.bank.commons.enums.TimePeriod;
import dev.enrique.bank.model.TransactionLimit;

public interface TransactionLimitRepository extends JpaRepository<TransactionLimit, Long> {
    @Query("""
            SELECT tl FROM TransactionLimit tl
            WHERE tl.account.accountNumber = :accountNumber
            AND tl.limitType = :limitType
            AND tl.timePeriod = :timePeriod
            ORDER BY tl.creationDate DESC
            LIMIT 1
            """)
    Optional<TransactionLimit> findLatestByAccountNumberAndLimitTypeAndTimePeriod(
            @Param("accountNumber") String accountNumber,
            @Param("limitType") LimitType limitType,
            @Param("timePeriod") TimePeriod timePeriod);

    @Query("""
            SELECT tl FROM TransactionLimit tl
            WHERE tl.account.accountNumber = :accountNumber
            AND tl.limitType = :limitType
            ORDER BY tl.creationDate DESC
            LIMIT 1
            """)
    Optional<TransactionLimit> findLatestByAccountNumberAndLimitType(
            @Param("accountNumber") String accountNumber,
            @Param("limitType") LimitType limitType);
}
