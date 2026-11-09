package dev.enrique.bank.dao;

import java.math.BigDecimal;

/** How many movements and how much money an account has used in a period. */
public record MovementUsage(long count, BigDecimal total) {
    public MovementUsage {
        total = total == null ? BigDecimal.ZERO : total;
    }
}
