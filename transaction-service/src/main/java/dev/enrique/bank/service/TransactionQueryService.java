package dev.enrique.bank.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;

import dev.enrique.bank.dto.response.HeaderResponse;
import dev.enrique.bank.dto.response.TransactionBasicResponse;
import dev.enrique.bank.dto.response.TransactionCommonResponse;
import dev.enrique.bank.dto.response.TransactionDetailedResponse;

public interface TransactionQueryService {
    List<TransactionDetailedResponse> getTransactionHistory(Long accountId);

    HeaderResponse<TransactionCommonResponse> getAllTransactions(Long accountId, Pageable pageable);

    List<TransactionCommonResponse> getTransactionByYearAndAccount(Long accountId, Integer year);

    List<TransactionBasicResponse> getAllTransactionsReversals(Long accountId);

    List<TransactionCommonResponse> getAllTransactionsFromAccounts(List<Long> accountIds);

    Optional<TransactionBasicResponse> findMaxTransaction(Long accountId);
}
