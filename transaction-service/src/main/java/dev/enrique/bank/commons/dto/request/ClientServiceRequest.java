package dev.enrique.bank.commons.dto.request;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.Currency;

public record ClientServiceRequest(
        String accountNumber,
        BigDecimal amount,
        Currency currency) {
}
