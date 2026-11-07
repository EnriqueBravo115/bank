package dev.enrique.bank.commons.dto.request;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.SecurityVerificationMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record ClientWithdrawalRequest(
        @NotBlank String accountNumber,
        @NotNull @Positive BigDecimal amount,
        SecurityVerificationMethod securityVerificationMethod,
        @PositiveOrZero BigDecimal transactionFee) {
}
