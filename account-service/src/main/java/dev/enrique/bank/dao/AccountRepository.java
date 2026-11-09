package dev.enrique.bank.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import dev.enrique.bank.commons.dto.response.AccountDetailedResponse;
import dev.enrique.bank.model.Account;
import jakarta.persistence.LockModeType;

public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByClabe(String clabe);

    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.AccountDetailedResponse(
                a.id, a.userId, a.email, a.clabe, a.accountNumber,
                a.accountType, a.accountStatus, a.currency, a.creationDate)
            FROM Account a
            WHERE a.accountNumber = :accountNumber
            """)
    Optional<AccountDetailedResponse> findByAccountNumber(String accountNumber);

    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.AccountDetailedResponse(
                a.id, a.userId, a.email, a.clabe, a.accountNumber,
                a.accountType, a.accountStatus, a.currency, a.creationDate)
            FROM Account a
            WHERE a.clabe = :clabe
            """)
    Optional<AccountDetailedResponse> findByClabe(String clabe);

    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.AccountDetailedResponse(
                a.id, a.userId, a.email, a.clabe, a.accountNumber,
                a.accountType, a.accountStatus, a.currency, a.creationDate)
            FROM Account a
            WHERE a.email = :email
            """)
    Optional<AccountDetailedResponse> findByEmail(String email);

    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.AccountDetailedResponse(
                a.id, a.userId, a.email, a.clabe, a.accountNumber,
                a.accountType, a.accountStatus, a.currency, a.creationDate)
            FROM Account a
            WHERE a.userId = :userId
            """)
    Optional<AccountDetailedResponse> findByUserId(String userId);

    Optional<Account> findEntityByAccountNumber(String accountNumber);

    // SELECT ... FOR UPDATE: every change of balance holds this lock until its transaction ends, so
    // two movements of the same account read and write the balance one after the other.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> lockByAccountNumber(String accountNumber);

    Optional<Account> findEntityByUserId(String userId);

    boolean existsByEmail(String email);

    boolean existsByUserId(String userId);

    boolean existsByAccountNumber(String accountNumber);
}
