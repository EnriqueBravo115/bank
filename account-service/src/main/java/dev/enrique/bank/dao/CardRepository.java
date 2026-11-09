package dev.enrique.bank.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import dev.enrique.bank.commons.enums.CardStatus;
import dev.enrique.bank.model.Card;

public interface CardRepository extends JpaRepository<Card, Long> {
    boolean existsByCardNumber(String cardNumber);

    @Query("SELECT c FROM Card c JOIN FETCH c.account WHERE c.cardNumber = :cardNumber")
    Optional<Card> findByCardNumber(String cardNumber);

    @Query("SELECT c FROM Card c JOIN FETCH c.account WHERE c.id = :id")
    Optional<Card> findWithAccountById(Long id);

    @Query("SELECT c FROM Card c WHERE c.account.userId = :userId ORDER BY c.creationDate DESC")
    List<Card> findAllByUserId(String userId);

    boolean existsByAccountIdAndCardStatusIn(Long accountId, Collection<CardStatus> statuses);
}
