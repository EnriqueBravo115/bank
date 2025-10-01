package dev.enrique.bank.commons.dto.request;

import dev.enrique.bank.commons.enums.TransactionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FilterStatusRequest(
        @NotBlank(message = "Account number is required")
        @Size(min = 16, max = 16, message = "Account number must be exactly 16 characters")
        String accountNumber,

        @NotNull(message = "Transaction status is required")
        TransactionStatus status) {
}
