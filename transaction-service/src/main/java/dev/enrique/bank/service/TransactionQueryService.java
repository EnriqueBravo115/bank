package dev.enrique.bank.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;

import dev.enrique.bank.commons.dto.response.HeaderResponse;
import dev.enrique.bank.commons.dto.response.TransactionCommonResponse;
import dev.enrique.bank.commons.dto.response.TransactionDetailedResponse;

public interface TransactionQueryService {
    List<TransactionDetailedResponse> getTransactionHistory(Long accountId);
    HeaderResponse<TransactionCommonResponse> getAllTransactions(Long accountId, Pageable pageable);
    List<TransactionDetailedResponse> getTransactionByYearAndAccount(Long accountId, Integer year);
    List<TransactionCommonResponse> getAllTransactionsReversals(Long accountId);
    List<TransactionCommonResponse> getAllTransactionsFromAccounts(List<Long> accountIds);
    Optional<TransactionCommonResponse> findMaxTransaction(Long accountId);
}
