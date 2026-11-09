package dev.enrique.bank.commons.dto.response;

import java.time.LocalDate;

import dev.enrique.bank.commons.enums.CardStatus;
import dev.enrique.bank.commons.enums.CardType;

// Only returned when the card is issued: the full number and the cvv are never shown again.
public record IssuedCardResponse(
        Long id,
        String cardNumber,
        String cardHolderName,
        LocalDate expirationDate,
        String cvv,
        CardType cardType,
        CardStatus cardStatus) {
}
