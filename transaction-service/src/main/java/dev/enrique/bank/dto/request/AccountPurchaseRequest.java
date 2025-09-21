package dev.enrique.bank.dto.request;

import java.math.BigDecimal;

public record AccountPurchaseRequest(
        BigDecimal amount,
        String sourceCardNumber,
        String cvv) {
}
