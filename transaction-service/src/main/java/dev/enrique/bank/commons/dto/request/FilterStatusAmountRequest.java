package dev.enrique.bank.commons.dto.request;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.TransactionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FilterStatusAmountRequest(
    @NotBlank(message = "Account number is required")
    String accountNumber,
    
    @NotNull(message = "Transaction status is required")
    TransactionStatus status,
    
    @NotNull(message = "Amount threshold is required")
    @Positive(message = "Amount threshold must be positive")
    BigDecimal amount
) {}
