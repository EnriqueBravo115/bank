package dev.enrique.bank.commons.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import dev.enrique.bank.commons.enums.Currency;

// The current balance: the latest account_balance row of the account.
public record BalanceResponse(
        String accountNumber,
        BigDecimal balance,
        Currency currency,
        LocalDateTime updatedAt) {
}
