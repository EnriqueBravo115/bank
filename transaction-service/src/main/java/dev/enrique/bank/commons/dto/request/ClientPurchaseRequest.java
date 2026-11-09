package dev.enrique.bank.commons.dto.request;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.Currency;

public record ClientPurchaseRequest(
        String accountNumber,
        BigDecimal amount,
        String sourceCardNumber,
        String cvv,
        Currency currency) {
}
