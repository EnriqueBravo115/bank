package dev.enrique.bank.commons.dto.request;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Money that comes into the account from outside the bank (e.g. cash at a branch).
public record ClientDepositRequest(
        @NotBlank String accountNumber,
        @NotNull @Positive BigDecimal amount,
        @NotNull Currency currency) {
}
