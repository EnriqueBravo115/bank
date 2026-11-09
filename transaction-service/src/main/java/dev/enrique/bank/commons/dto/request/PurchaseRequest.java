package dev.enrique.bank.commons.dto.request;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PurchaseRequest(
        @NotNull @Positive BigDecimal amount,
        @NotBlank String description,
        @NotBlank String sourceAccountNumber,
        @NotBlank String sourceCardNumber,
        @NotBlank String merchantCode,
        String merchantCategory,
        String posId,
        @NotBlank String cvv,
        @NotNull Currency currency) {
}
