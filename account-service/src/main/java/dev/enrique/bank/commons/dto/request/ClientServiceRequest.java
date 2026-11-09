package dev.enrique.bank.commons.dto.request;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ClientServiceRequest(
        @NotBlank String accountNumber,
        @NotNull @Positive BigDecimal amount,
        @NotNull Currency currency) {
}
