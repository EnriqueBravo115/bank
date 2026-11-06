package dev.enrique.bank.commons.dto.request;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.SecurityVerificationMethod;
import dev.enrique.bank.commons.enums.WithdrawalMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record WithdrawalRequest(
        @NotNull @Positive BigDecimal amount,
        @NotBlank String description,
        @NotBlank String sourceAccountNumber,
        String atmLocation,
        String atmSessionId,
        String receiptNumber,
        String branchCode,
        String tellerId,
        @PositiveOrZero BigDecimal transactionFee,
        @NotNull Currency currency,
        @NotNull SecurityVerificationMethod securityVerificationMethod,
        @NotNull WithdrawalMethod withdrawalMethod) {
}
