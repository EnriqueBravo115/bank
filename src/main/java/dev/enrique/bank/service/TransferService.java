package dev.enrique.bank.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Pageable;
import org.springframework.data.domain.Page;

import dev.enrique.bank.dto.request.TransferRequest;
import dev.enrique.bank.model.Transaction;

public interface TransferService {
    void transfer(Long sourceAccountId, Long targetAccountId, BigDecimal amount);

    Map<String, Map<String, List<TransferRequest>>> getTransferHistory(Long accountId);

    void reverseTransfer(Long transactionId);

    boolean hasSufficientFunds(Long accountId, BigDecimal amount);

    void scheduleTransfer(TransferRequest request, LocalDateTime scheduleDate);

    Transaction getTransferDetails(Long transactionId);

    void cancelScheduledTransfer(Long transactionId);

    void transferBetweenUsers(Long sourceId, Long targetUserId, BigDecimal amount);

    BigDecimal calculateTransferFee(BigDecimal amount, String currency);

    void urgentTransfer(TransferRequest request);

    BigDecimal getTransferLimit(Long accountId);

    void notifyTransfer(Long transactionId);

    void validateTransfer(TransferRequest request);

    Page<Transaction> getAllTransfers(Pageable pageable);
}
