package dev.enrique.bank.commons.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;

public record TransactionResponse(
        String transactionCode,
        BigDecimal amount,
        String description,
        LocalDateTime transactionDate,
        TransactionType transactionType,
        TransactionStatus transactionStatus) {
}
