package dev.enrique.bank.dao.projection;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.TransactionType;

public interface TransactionBasicProjection {
    TransactionType getTransactionType();
    BigDecimal getAmount();
}
