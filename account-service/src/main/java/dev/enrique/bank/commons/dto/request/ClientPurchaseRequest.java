package dev.enrique.bank.commons.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ClientPurchaseRequest(
        @NotBlank String accountNumber,
        @NotNull @Positive BigDecimal amount,
        String sourceCardNumber,
        String cvv) {
}
