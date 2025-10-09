package dev.enrique.bank.commons.dto.request;

import dev.enrique.bank.commons.enums.TransactionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record FilterStatusAmountRequest(
        @NotBlank(message = "Source identifier is required")
        String sourceIdentifier,

        @NotNull(message = "Transaction status is required")
        TransactionStatus status,

        @NotNull(message = "Amount threshold is required")
        @Positive(message = "Amount threshold must be positive")
        BigDecimal amount) {
}
