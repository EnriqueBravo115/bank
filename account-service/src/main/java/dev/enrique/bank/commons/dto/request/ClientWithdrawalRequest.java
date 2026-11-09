package dev.enrique.bank.commons.dto.request;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.SecurityVerificationMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

// The fee is debited along with the amount; the verification method is recorded with the movement.
public record ClientWithdrawalRequest(
        @NotBlank String accountNumber,
        @NotNull @Positive BigDecimal amount,
        @NotNull SecurityVerificationMethod securityVerificationMethod,
        @PositiveOrZero BigDecimal transactionFee,
        @NotNull Currency currency) {
}
