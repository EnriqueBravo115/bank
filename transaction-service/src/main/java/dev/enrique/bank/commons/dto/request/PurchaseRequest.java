package dev.enrique.bank.commons.dto.request;

import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.TransactionType;

import java.math.BigDecimal;

public record PurchaseRequest(
        BigDecimal amount,
        String description,
        TransactionType transactionType,
        String sourceCardNumber,
        String merchantCode,
        String merchantCategory,
        String posId,
        String cvv,
        Currency currency) {
}
