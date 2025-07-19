package dev.enrique.bank.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import dev.enrique.bank.dto.request.TransferRequest;

public interface TransferService {
    void transfer(Long sourceAccountId, Long targetAccountId, BigDecimal amount);

    void reverseTransfer(Long transactionId);

    void scheduleTransfer(TransferRequest request, LocalDateTime scheduleDate);

    void cancelScheduledTransfer(Long transactionId);
}
