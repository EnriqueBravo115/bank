package dev.enrique.bank.commons.dto.response;

import java.time.LocalDate;

import dev.enrique.bank.commons.enums.CardStatus;
import dev.enrique.bank.commons.enums.CardType;
import dev.enrique.bank.model.Card;

public record CardResponse(
        Long id,
        String maskedNumber,
        String cardHolderName,
        LocalDate expirationDate,
        CardType cardType,
        CardStatus cardStatus) {

    public static CardResponse from(Card card) {
        return new CardResponse(card.getId(), card.maskedNumber(), card.getCardHolderName(),
                card.getExpirationDate(), card.getCardType(), card.getCardStatus());
    }
}
