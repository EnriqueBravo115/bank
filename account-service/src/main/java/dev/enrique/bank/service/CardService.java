package dev.enrique.bank.service;

import java.util.List;

import dev.enrique.bank.commons.dto.response.AccountDetailedResponse;
import dev.enrique.bank.commons.dto.response.CardResponse;
import dev.enrique.bank.commons.dto.response.IssuedCardResponse;
import dev.enrique.bank.model.Account;

public interface CardService {
    IssuedCardResponse issue(String userId, String cardHolderName);

    List<CardResponse> findByUserId(String userId);

    // ownerScope: the user whose cards may be changed, or null for an admin
    CardResponse block(Long cardId, String ownerScope);

    CardResponse unblock(Long cardId, String ownerScope);

    CardResponse cancel(Long cardId, String ownerScope);

    AccountDetailedResponse findAccountByCardNumber(String cardNumber);

    /** Throws CardDeclinedException unless the card can pay from this account. */
    void verifyPayment(Account account, String cardNumber, String cvv);
}
