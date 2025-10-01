package dev.enrique.bank.commons.dto.request;

import java.math.BigDecimal;

public record ClientPurchaseRequest(
        BigDecimal amount,
        String sourceCardNumber,
        String cvv) {
}
