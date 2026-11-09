package dev.enrique.bank.commons.dto.request;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.LimitType;
import dev.enrique.bank.commons.enums.TimePeriod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Within each period the account can make at most maxTransactions movements of the type, adding up
// to at most maxAmount (in the account's currency).
public record LimitRequest(
        @NotBlank String accountNumber,
        @NotNull LimitType limitType,
        @NotNull TimePeriod timePeriod,
        @NotNull @Positive BigDecimal maxAmount,
        @NotNull @Positive Integer maxTransactions) {
}
