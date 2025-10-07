package dev.enrique.bank.dao.projection;

import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface TransactionCommonProjection {
    BigDecimal getAmount();
    LocalDateTime getTransactionDate();
    TransactionType getTransactionType();
    TransactionStatus getTransactionStatus();
}
