package dev.enrique.bank.commons.dto.request;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// The currency must be the one of both accounts.
public record ClientTransferRequest(
        @NotNull @Positive BigDecimal amount,
        @NotBlank String sourceAccountNumber,
        @NotBlank String targetAccountNumber,
        @NotNull Currency currency) {
}
