package dev.enrique.bank.commons.dto.request;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// The cvv must match the one stored with the card.
public record ClientPurchaseRequest(
        @NotBlank String accountNumber,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String sourceCardNumber,
        String cvv,
        @NotNull Currency currency) {
}
