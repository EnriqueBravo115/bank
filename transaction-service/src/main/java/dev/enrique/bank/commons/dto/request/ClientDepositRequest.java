package dev.enrique.bank.commons.dto.request;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.Currency;

public record ClientDepositRequest(
        String accountNumber,
        BigDecimal amount,
        Currency currency) {
}
