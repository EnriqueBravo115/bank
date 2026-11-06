package dev.enrique.bank.commons.dto.request;

import java.math.BigDecimal;

public record ClientPurchaseRequest(
        String accountNumber,
        BigDecimal amount,
        String sourceCardNumber,
        String cvv) {
}
