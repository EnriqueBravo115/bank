package dev.enrique.bank.service;

import java.math.BigDecimal;

public interface TransactionCreationService {
    void transfer(Long sourceAccountId, Long targetAccountId, BigDecimal amount);
    void reverseTransfer(Long transactionId);
}
