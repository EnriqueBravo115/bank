package dev.enrique.bank.service.impl;

import static dev.enrique.bank.commons.constants.ErrorMessage.ACCOUNT_NOT_FOUND;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.Status;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.commons.exception.ApiRequestException;
import dev.enrique.bank.dao.AccountRepository;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.dao.projection.TransactionCommonProjection;
import dev.enrique.bank.dao.projection.TransactionDetailedProjection;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.model.Transaction;
import dev.enrique.bank.service.TransactionService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    @Override
    public List<TransactionDetailedProjection> getTransactionHistory(Long accountId) {
        return transactionRepository.findAllTransactionsInvolvingAccount(accountId);
    }

    @Override
    public List<TransactionCommonProjection> getTransactionByYearAndAccount(Long accountId, Integer year) {
        return transactionRepository.findTransactionsByYearAndAccount(accountId, year);
    }

    @Override
    public Page<TransactionCommonProjection> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAllTransactions(pageable);
    }

    @Override
    public List<Transaction> getAllTransactionsWithReversals() {
        return transactionRepository.findAll().stream()
                .flatMap(transaction -> {
                    Stream<Transaction> mainTransaction = Stream.of(transaction);
                    Stream<Transaction> reversals = transaction.getReversals() != null
                            ? transaction.getReversals().stream()
                            : Stream.empty();
                    return Stream.concat(mainTransaction, reversals);
                }).collect(Collectors.toList());
    }

    @Override
    public BigDecimal calculateTotalTransactionAmount() {
        return transactionRepository.findAll().stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal calculateTotalAmountByType(TransactionType type) {
        return transactionRepository.findAll().stream()
                .filter(t -> t.getTransactionType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal calculateTotalAmountByAccount(Long accountId) {
        return transactionRepository.findAllByAccountId(accountId).stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public boolean hasSufficientFunds(Long accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ApiRequestException(ACCOUNT_NOT_FOUND, HttpStatus.NOT_FOUND));

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
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ApiRequestException(ACCOUNT_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (account.getStatus() != Status.OPEN) {
            throw new IllegalStateException("Account is not open");
        }

        return account.getBalance().multiply(new BigDecimal("2"));
    }
}
