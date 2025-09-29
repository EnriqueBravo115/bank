package dev.enrique.bank.dao.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;

public interface TransactionDetailedProjection {
    String getTransactionCode();
    BigDecimal getAmount();
    String getDescription();
    LocalDateTime getTransactionDate();
    TransactionType getTransactionType();
    TransactionStatus getTransactionStatus();
}
