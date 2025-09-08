package dev.enrique.bank.dao.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import dev.enrique.bank.enums.TransactionStatus;
import dev.enrique.bank.enums.TransactionType;

public interface TransactionDetailedProjection {
    String getTransactionNumber();
    BigDecimal getAmount();
    String getDescription();
    LocalDateTime getTransactionDate();
    TransactionType getTransactionType();
    TransactionStatus getTransactionStatus();
}
