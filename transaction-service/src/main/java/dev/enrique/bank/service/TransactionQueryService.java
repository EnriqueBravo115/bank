package dev.enrique.bank.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;

import dev.enrique.bank.commons.dto.response.HeaderResponse;
import dev.enrique.bank.commons.dto.response.TransactionCommonResponse;
import dev.enrique.bank.commons.dto.response.TransactionDetailedResponse;
import dev.enrique.bank.commons.enums.TransactionStatus;

public interface TransactionQueryService {
    List<TransactionDetailedResponse> getTransactionHistory(String accountNumber, TransactionStatus status);
    HeaderResponse<TransactionCommonResponse> getAllTransactions(String accountNumber, TransactionStatus status,
            Pageable pageable);
    List<TransactionDetailedResponse> getTransactionByYear(String accountNumber, Integer year);
    List<TransactionCommonResponse> getAllTransactionsFromAccounts(List<String> accountNumbers);
    Optional<TransactionCommonResponse> findMaxTransaction(String accountNumber);
}
