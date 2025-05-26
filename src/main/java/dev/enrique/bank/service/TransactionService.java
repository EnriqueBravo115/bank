package dev.enrique.bank.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.dao.projection.TransactionBasicProjection;
import dev.enrique.bank.dao.projection.TransactionCommonProjection;
import dev.enrique.bank.dao.projection.TransactionDetailedProjection;

public interface TransactionService {
    public List<TransactionDetailedProjection> getTransactionHistory(Long accountId);

    List<TransactionCommonProjection> getTransactionByYearAndAccount(Long accountId, Integer year);

    Page<TransactionCommonProjection> getAllTransactions(Long accountId, Pageable pageable);

    List<TransactionBasicProjection> getAllTransactionsReversals(Long accountId);

    BigDecimal getTransferLimit(Long accountId);

    BigDecimal calculateTotalTransactionAmount(Long accountId);

    BigDecimal calculateTotalAmountByType(Long accountId, TransactionType type);

    Boolean hasSufficientFunds(Long accountId, BigDecimal amount);

    BigDecimal calculateTransferFee(BigDecimal amount, Currency currency);
}
