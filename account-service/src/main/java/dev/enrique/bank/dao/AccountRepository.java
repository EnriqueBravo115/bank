package dev.enrique.bank.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import dev.enrique.bank.commons.dto.response.AccountDetailedResponse;
import dev.enrique.bank.model.Account;

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

    Optional<Account> findEntityByAccountNumber(String accountNumber);

    boolean existsByEmail(String email);
}
