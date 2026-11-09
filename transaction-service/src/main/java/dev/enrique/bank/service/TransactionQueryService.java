package dev.enrique.bank.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import dev.enrique.bank.commons.dto.response.TransactionResponse;
import dev.enrique.bank.commons.enums.IdentifierType;
import dev.enrique.bank.commons.enums.TransactionStatus;

public interface TransactionQueryService {
    List<TransactionResponse> getTransactionHistory(String accountNumber, TransactionStatus status);

    Page<TransactionResponse> getAllTransactions(String accountNumber, TransactionStatus status, Pageable pageable);

    List<TransactionResponse> getTransactionsByYear(String accountNumber, TransactionStatus status, Integer year);

    List<TransactionResponse> getAllTransactionsByAccountNumbers(List<String> accountNumbers);

    TransactionResponse getMaxTransaction(String accountNumber, TransactionStatus status);

    List<TransactionResponse> getTransactionsInDateRange(String accountNumber, TransactionStatus status,
            LocalDateTime startDate, LocalDateTime endDate);

    TransactionResponse getTransactionByCode(String transactionCode);

    List<TransactionResponse> getAllTransactionByAmountRangeAndStatus(String accountNumber,
            TransactionStatus status, BigDecimal minAmount, BigDecimal maxAmount);

    List<TransactionResponse> getAllByIdentifierTypeAndStatus(String accountNumber,
            IdentifierType identifierType, TransactionStatus status);

    List<TransactionResponse> getTransactionByKeyword(String accountNumber, TransactionStatus status,
            String keyword);
}
