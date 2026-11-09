package dev.enrique.bank.commons.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import dev.enrique.bank.commons.enums.TransactionDirection;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;

/** A transaction as seen from one account: direction says whether the money left it or entered it. */
public record TransactionResponse(
        String transactionCode,
        BigDecimal amount,
        String description,
        LocalDateTime transactionDate,
        TransactionType transactionType,
        TransactionStatus transactionStatus,
        TransactionDirection direction) {

    // Used by the JPQL constructor expressions (TransactionRepository): credit is a CASE expression.
    public TransactionResponse(String transactionCode, BigDecimal amount, String description,
            LocalDateTime transactionDate, TransactionType transactionType, TransactionStatus transactionStatus,
            Boolean credit) {
        this(transactionCode, amount, description, transactionDate, transactionType, transactionStatus,
                Boolean.TRUE.equals(credit) ? TransactionDirection.CREDIT : TransactionDirection.DEBIT);
    }
}
