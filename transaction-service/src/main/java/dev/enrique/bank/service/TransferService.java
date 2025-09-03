package dev.enrique.bank.service;

import java.math.BigDecimal;

public interface TransferService {
    void transfer(Long sourceAccountId, Long targetAccountId, BigDecimal amount);

    void reverseTransfer(Long transactionId);
}
