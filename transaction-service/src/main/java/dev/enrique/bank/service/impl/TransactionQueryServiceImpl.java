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
    public List<TransactionResponse> getTransactionHistory(String sourceIdentifier, TransactionStatus status) {
        return transactionRepository.findAllBySourceIdentifierAndStatus(currentCaller.ownerScope(), sourceIdentifier, status);
    }

    @Override
    public Page<TransactionResponse> getAllTransactions(String sourceIdentifier, TransactionStatus status,
            Pageable pageable) {
        return transactionRepository.findPageBySourceIdentifierAndStatus(currentCaller.ownerScope(), sourceIdentifier, status, pageable);
    }

    @Override
    public List<TransactionResponse> getTransactionsByYear(String sourceIdentifier, TransactionStatus status,
            Integer year) {
        return transactionRepository.findAllBySourceIdentifierAndYear(currentCaller.ownerScope(), sourceIdentifier, year, status);
    }

    @Override
    public List<TransactionResponse> getAllTransactionsBySourceIdentifiers(List<String> sourceIdentifiers) {
        return transactionRepository.findAllCompletedBySourceIdentifiersIn(sourceIdentifiers);
    }

    @Override
    public TransactionResponse getMaxTransaction(String sourceIdentifier, TransactionStatus status) {
        return transactionRepository.findMaxBySourceIdentifierAndStatus(currentCaller.ownerScope(), sourceIdentifier, status)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "No " + status + " transactions found for: " + sourceIdentifier));
    }

    @Override
    public List<TransactionResponse> getTransactionsInDateRange(String sourceIdentifier, TransactionStatus status,
            LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findAllBySourceIdentifierAndDateRangeAndStatus(currentCaller.ownerScope(), 
                sourceIdentifier, status, startDate, endDate);
    }

    @Override
    public TransactionResponse getTransactionByCode(String transactionCode) {
        return transactionRepository.findResponseByTransactionCode(currentCaller.ownerScope(), transactionCode)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Transaction not found with code: " + transactionCode));
    }

    @Override
    public List<TransactionResponse> getAllTransactionByAmountRangeAndStatus(String sourceIdentifier,
            TransactionStatus status, BigDecimal minAmount, BigDecimal maxAmount) {
        return transactionRepository.findAllByAmountRangeAndStatus(currentCaller.ownerScope(), sourceIdentifier, status, minAmount, maxAmount);
    }

    @Override
    public List<TransactionResponse> getAllByIdentifierTypeAndStatus(String sourceIdentifier,
            IdentifierType identifierType, TransactionStatus status) {
        return transactionRepository.findAllByIdentifierTypeAndStatus(currentCaller.ownerScope(), sourceIdentifier, identifierType, status);
    }

    @Override
    public List<TransactionResponse> getTransactionByKeyword(String sourceIdentifier, TransactionStatus status,
            String keyword) {
        return transactionRepository.searchByKeyword(currentCaller.ownerScope(), sourceIdentifier, status, keyword);
    }
}
