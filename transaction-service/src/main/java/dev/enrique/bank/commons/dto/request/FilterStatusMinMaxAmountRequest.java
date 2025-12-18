package dev.enrique.bank.commons.dto.request;

import dev.enrique.bank.commons.enums.TransactionStatus;

import java.math.BigDecimal;

public record FilterStatusMinMaxAmountRequest(
        String sourceIdentifier,
        TransactionStatus status,
        BigDecimal minAmount,
        BigDecimal maxAmount) {
}
