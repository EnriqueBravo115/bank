package dev.enrique.bank.service;

import java.math.BigDecimal;

public interface TransferService {
    void transfer(String sourceAccountNumber, String targetAccounNumber, BigDecimal amount);
    void reverseTransfer(Long transactionId);
}
