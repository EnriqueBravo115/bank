package dev.enrique.bank.commons.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.model.Transaction;

public record TransactionResultResponse(
        String transactionCode,
        BigDecimal amount,
        TransactionType transactionType,
        TransactionStatus transactionStatus,
        String reason,
        LocalDateTime transactionDate) {

    public static TransactionResultResponse from(Transaction transaction) {
        return new TransactionResultResponse(
                transaction.getTransactionCode(),
                transaction.getAmount(),
                transaction.getTransactionType(),
                transaction.getTransactionStatus(),
                transaction.getReason(),
                transaction.getTransactionDate());
    }
}
