package dev.enrique.bank.dao.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import dev.enrique.bank.enums.TransactionType;

public interface TransactionBasicProjection {
    Long getId();

    BigDecimal getAmount();

    TransactionType getTransactionType();

    LocalDateTime getTransactionDate();
}
