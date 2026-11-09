package dev.enrique.bank.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dev.enrique.bank.commons.enums.BalanceType;
import dev.enrique.bank.model.AccountBalance;

// The balance is the latest row by id: ids follow the order in which rows were inserted while the
// account was locked, whereas two rows can share a creation_date.
@Repository
public interface AccountBalanceRepository extends JpaRepository<AccountBalance, Long> {
    @Query("""
            SELECT ab FROM AccountBalance ab
            WHERE ab.account.accountNumber = :accountNumber
            ORDER BY ab.id DESC
            LIMIT 1
            """)
    Optional<AccountBalance> findLatestByAccountNumber(
            @Param("accountNumber") String accountNumber);

    @Query("""
            SELECT ab FROM AccountBalance ab
            WHERE ab.account.accountNumber = :accountNumber
            AND ab.balanceType = :type
            ORDER BY ab.id DESC
            LIMIT 1
            """)
    Optional<AccountBalance> findLatestByAccountNumberAndType(
            @Param("accountNumber") String accountNumber,
            @Param("type") BalanceType type);
}
