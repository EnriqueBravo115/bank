package dev.enrique.bank.commons.dto.request;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.IdentifierType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Money that comes from outside the bank (cash at a branch, payroll...) into an account.
public record DepositRequest(
        @NotNull @Positive BigDecimal amount,
        @NotBlank String description,
        @NotBlank String targetIdentifier,
        @NotNull IdentifierType targetIdentifierType,
        @NotNull Currency currency) {
}
