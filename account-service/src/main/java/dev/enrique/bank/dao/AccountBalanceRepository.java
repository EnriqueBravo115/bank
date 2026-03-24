package dev.enrique.bank.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dev.enrique.bank.commons.enums.BalanceType;
import dev.enrique.bank.model.AccountBalance;

@Repository
public interface AccountBalanceRepository extends JpaRepository<AccountBalance, Long> {
    @Query("""
            SELECT ab FROM AccountBalance ab
            WHERE ab.account.id = :accountId
            ORDER BY ab.creationDate DESC
            LIMIT 1
            """)
    Optional<AccountBalance> findLatestByAccountId(Long accountId);

    @Query("""
            SELECT ab FROM AccountBalance ab
            WHERE ab.account.id = :accountId
            AND ab.balanceType = :type
            ORDER BY ab.creationDate DESC
            LIMIT 1
            """)
    Optional<AccountBalance> findLatestByAccountIdAndType(
            @Param("accountId") Long accountId,
            @Param("type") BalanceType type);
}
