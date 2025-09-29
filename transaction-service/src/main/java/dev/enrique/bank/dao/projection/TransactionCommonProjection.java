package dev.enrique.bank.dao.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface TransactionCommonProjection {
    String getTransactionCode();
    BigDecimal getAmount();
    LocalDateTime getTransactionDate();
}
