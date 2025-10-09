package dev.enrique.bank.commons.dto.request;

import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FilterStatusTypeRequest(
        @NotBlank(message = "Source identifier is required")
        String sourceIdentifier,

        @NotNull(message = "Transaction status is required")
        TransactionStatus status,

        @NotNull(message = "Transaction type is required")
        TransactionType type) {
}
