package dev.enrique.bank.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import dev.enrique.bank.commons.dto.response.TransactionResponse;
import dev.enrique.bank.commons.enums.IdentifierType;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.exception.TransactionNotFoundException;
import dev.enrique.bank.config.CurrentCaller;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.service.TransactionQueryService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionQueryServiceImpl implements TransactionQueryService {
    private final TransactionRepository transactionRepository;
    private final CurrentCaller currentCaller;

    @Override
    public List<TransactionResponse> getTransactionHistory(String accountNumber, TransactionStatus status) {
        return transactionRepository.findAllByAccountNumberAndStatus(currentCaller.ownerScope(), accountNumber, status);
    }

    @Override
    public Page<TransactionResponse> getAllTransactions(String accountNumber, TransactionStatus status,
            Pageable pageable) {
        return transactionRepository.findPageByAccountNumberAndStatus(currentCaller.ownerScope(), accountNumber, status, pageable);
    }

    @Override
    public List<TransactionResponse> getTransactionsByYear(String accountNumber, TransactionStatus status,
            Integer year) {
        return transactionRepository.findAllByAccountNumberAndYear(currentCaller.ownerScope(), accountNumber, year, status);
    }

    @Override
    public List<TransactionResponse> getAllTransactionsByAccountNumbers(List<String> accountNumbers) {
        return transactionRepository.findAllCompletedByAccountNumberIn(accountNumbers);
    }

    @Override
    public TransactionResponse getMaxTransaction(String accountNumber, TransactionStatus status) {
        return transactionRepository.findMaxByAccountNumberAndStatus(currentCaller.ownerScope(), accountNumber, status)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "No " + status + " transactions found for: " + accountNumber));
    }

    @Override
    public List<TransactionResponse> getTransactionsInDateRange(String accountNumber, TransactionStatus status,
            LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findAllByAccountNumberAndDateRangeAndStatus(currentCaller.ownerScope(), 
                accountNumber, status, startDate, endDate);
    }

    @Override
    public TransactionResponse getTransactionByCode(String transactionCode) {
        return transactionRepository.findResponseByTransactionCode(currentCaller.ownerScope(), transactionCode)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Transaction not found with code: " + transactionCode));
    }

    @Override
    public List<TransactionResponse> getAllTransactionByAmountRangeAndStatus(String accountNumber,
            TransactionStatus status, BigDecimal minAmount, BigDecimal maxAmount) {
        return transactionRepository.findAllByAmountRangeAndStatus(currentCaller.ownerScope(), accountNumber, status, minAmount, maxAmount);
    }

    @Override
    public List<TransactionResponse> getAllByIdentifierTypeAndStatus(String accountNumber,
            IdentifierType identifierType, TransactionStatus status) {
        return transactionRepository.findAllByIdentifierTypeAndStatus(currentCaller.ownerScope(), accountNumber, identifierType, status);
    }

    @Override
    public List<TransactionResponse> getTransactionByKeyword(String accountNumber, TransactionStatus status,
            String keyword) {
        return transactionRepository.searchByKeyword(currentCaller.ownerScope(), accountNumber, status, keyword);
    }
}
