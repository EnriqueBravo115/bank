package dev.enrique.bank.commons.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ClientTransferRequest(
        @NotNull @Positive BigDecimal amount,
        @NotBlank String sourceAccountNumber,
        @NotBlank String targetAccountNumber) {
}
