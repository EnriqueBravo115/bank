package dev.enrique.bank.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.dao.projection.TransactionCommonProjection;
import dev.enrique.bank.dao.projection.TransactionDetailedProjection;
import dev.enrique.bank.model.Transaction;

public interface TransactionService {
    public List<TransactionDetailedProjection> getTransactionHistory(Long accountId);

    List<TransactionCommonProjection> getTransactionByYearAndAccount(Long accountId, Integer year);

    Page<TransactionCommonProjection> getAllTransactions(Pageable pageable);

    List<Transaction> getAllTransactionsWithReversals();

    BigDecimal getTransferLimit(Long accountId);

    BigDecimal calculateTotalTransactionAmount();

    BigDecimal calculateTotalAmountByType(TransactionType type);

    BigDecimal calculateTotalAmountByAccount(Long accountId);

    boolean hasSufficientFunds(Long accountId, BigDecimal amount);

    BigDecimal calculateTransferFee(BigDecimal amount, Currency currency);
}
