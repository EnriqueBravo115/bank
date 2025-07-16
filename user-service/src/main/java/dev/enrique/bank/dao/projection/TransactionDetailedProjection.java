package dev.enrique.bank.dao.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface TransactionDetailedProjection {
    Long getId();

    BigDecimal getAmount();

    LocalDateTime getTransactionDate();

    String getDescription();

    String getTransactionType();
}
