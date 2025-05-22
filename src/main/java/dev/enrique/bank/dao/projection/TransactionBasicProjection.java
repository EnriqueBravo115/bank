package dev.enrique.bank.dao.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface TransactionBasicProjection {
    Long getId();

    BigDecimal getAmount();

    String getDescription();

    LocalDateTime getTransactionDate();
}
