package dev.enrique.bank.dto.response;

import dev.enrique.bank.commons.enums.TransactionStatus;
import jakarta.validation.constraints.NotBlank;

public record MovementResultResponse(
        @NotBlank TransactionStatus transactionStatus,
        @NotBlank String reason) {
}
