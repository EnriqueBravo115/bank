package dev.enrique.bank.dao.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;

public interface TransactionDetailedProjection {
    Long getId();

    BigDecimal getAmount();

    LocalDateTime getTransactionDate();

    String getDescription();

    //@Value("#{target.sourceAccount != null ? target.sourceAccount.accountNumber : null}")
    //String getSourceAccountNumber();

    //@Value("#{target.targetAccount != null ? target.targetAccount.accountNumber : null}")
    //String getTargetAccountNumber();

    //@Value("#{target.transactionType != null ? target.transactionType.name().toLowerCase() : null}")
    String getTransactionType();
}
