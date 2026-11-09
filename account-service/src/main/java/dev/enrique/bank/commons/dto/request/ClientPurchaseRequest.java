package dev.enrique.bank.commons.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// The cvv is checked against the card and never stored. transaction-service only sends it on its first
// attempt: a retry that never reached account-service before is declined.
public record ClientPurchaseRequest(
        @NotBlank String accountNumber,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String sourceCardNumber,
        String cvv) {
}
