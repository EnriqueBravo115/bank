package dev.enrique.bank.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.Status;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.dao.projection.TransactionBasicProjection;
import dev.enrique.bank.dao.projection.TransactionCommonProjection;
import dev.enrique.bank.dao.projection.TransactionDetailedProjection;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.model.Transaction;
import dev.enrique.bank.service.TransactionService;
import dev.enrique.bank.service.util.AccountHelper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountHelper accountHelper;

    @Override
    public List<TransactionDetailedProjection> getTransactionHistory(Long accountId) {
        accountHelper.validateAccountId(accountId);
        return transactionRepository.findAllTransactions(accountId, TransactionDetailedProjection.class);
    }

    @Override
    public List<TransactionCommonProjection> getTransactionByYearAndAccount(Long accountId, Integer year) {
        accountHelper.validateAccountIdAndYear(accountId, year);
        return transactionRepository.findTransactionsByYearAndAccount(accountId, year);
    }

    @Override
    public Page<TransactionCommonProjection> getAllTransactions(Long accountId, Pageable pageable) {
        accountHelper.validateAccountId(accountId);
        return transactionRepository.findAllTransactions(accountId, pageable);
    }

    @Override
    public List<TransactionBasicProjection> getAllTransactionsReversals(Long accountId) {
        accountHelper.validateAccountId(accountId);
        return transactionRepository.findByTransactionReversal(accountId);
    }

    @Override
    public BigDecimal calculateTotalTransactionAmount(Long accountId) {
        return transactionRepository.findAllTransactions(accountId, Transaction.class).stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal calculateTotalAmountByType(Long accountId, TransactionType type) {
        return transactionRepository.findAllTransactions(accountId, Transaction.class).stream()
                .filter(t -> t.getTransactionType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public Boolean hasSufficientFunds(Long accountId, BigDecimal amount) {
        Account account = accountHelper.getAccountById(accountId);
        return account.getBalance().compareTo(amount) > 0;
    }

    @Override
    public BigDecimal calculateTransferFee(BigDecimal amount, Currency currency) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Amount must be greater than zero");

        BigDecimal feePercentage = currency.getFeePercentage();
        BigDecimal minimumFee = currency.getMinimumFee();
        BigDecimal maximumFee = currency.getMaximumFee();

        BigDecimal calculatedFee = amount.multiply(feePercentage);

        if (calculatedFee.compareTo(minimumFee) < 0) {
            calculatedFee = minimumFee;
        } else if (calculatedFee.compareTo(maximumFee) > 0) {
            calculatedFee = maximumFee;
        }

        calculatedFee = calculatedFee.setScale(2, RoundingMode.HALF_UP);

        return calculatedFee;
    }

    @Override
    public BigDecimal getTransferLimit(Long accountId) {
        Account account = accountHelper.getAccountById(accountId);

        if (account.getStatus() != Status.OPEN)
            throw new IllegalStateException("Account is not open");

        return account.getBalance().multiply(new BigDecimal("2"));
    }
}
