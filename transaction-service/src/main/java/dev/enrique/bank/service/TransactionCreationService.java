package dev.enrique.bank.service;

import java.math.BigDecimal;

import dev.enrique.bank.dto.request.AccountTransferRequest;

public interface TransactionCreationService {
    void transferBetweenAccounts(AccountTransferRequest accountTransferRequest);
    void createTransaction(String sourceAccountNumber, String targetAccounNumber, BigDecimal amount);
    void reverseTransaction(Long transactionId);
}
