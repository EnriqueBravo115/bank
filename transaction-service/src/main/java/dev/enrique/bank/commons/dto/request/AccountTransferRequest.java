package dev.enrique.bank.commons.dto.request;

import java.math.BigDecimal;

public record AccountTransferRequest(
        BigDecimal amount,
        String sourceAccountNumber,
        String targetAccountNumber) {
}
