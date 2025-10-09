package dev.enrique.bank.commons.dto.request;

import dev.enrique.bank.commons.enums.TransactionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FilterStatusRequest(
        @NotBlank(message = "Source identifier is required")
        String sourceIdentifier,

        @NotNull(message = "Transaction status is required")
        TransactionStatus status) {
}
