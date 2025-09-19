package dev.enrique.bank.dto.request;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record AccountTransferRequest(
        @Positive BigDecimal amount,
        @NotBlank String description,
        @NotBlank TransactionType transactionType,
        @NotBlank Currency currency,
        @NotBlank String sourceAccount,
        @NotBlank String targetAccount) {
}
