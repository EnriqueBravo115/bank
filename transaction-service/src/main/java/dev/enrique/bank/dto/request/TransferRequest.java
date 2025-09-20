package dev.enrique.bank.dto.request;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record TransferRequest(
        @Positive BigDecimal amount,
        @NotBlank String description,
        @NotBlank Currency currency,
        @NotBlank String sourceAccountNumber,
        @NotBlank String targetAccountNumber) {
}
