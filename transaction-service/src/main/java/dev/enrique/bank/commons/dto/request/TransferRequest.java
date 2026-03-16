package dev.enrique.bank.commons.dto.request;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.IdentifierType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransferRequest(
        @Positive BigDecimal amount,
        @NotBlank String description,
        @NotNull Currency currency,
        @NotBlank String sourceIdentifier,
        @NotNull IdentifierType sourceIdentifierType,
        @NotBlank String targetIdentifier,
        @NotNull IdentifierType targetIdentifierType) {
}
