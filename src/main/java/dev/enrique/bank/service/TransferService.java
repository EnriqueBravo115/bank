package dev.enrique.bank.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.dto.request.TransferRequest;
import dev.enrique.bank.dto.response.TransferResponse;
import dev.enrique.bank.model.Transaction;

public interface TransferService {
    Map<String, Map<String, List<TransferResponse>>> getTransferHistory(Long accountId);

    List<Transaction> getTransactionByYear(Integer year);

    Page<Transaction> getAllTransfers(Pageable pageable);

    void transfer(Long sourceAccountId, Long targetAccountId, BigDecimal amount);

    void reverseTransfer(Long transactionId);

    boolean hasSufficientFunds(Long accountId, BigDecimal amount);

    void scheduleTransfer(TransferRequest request, LocalDateTime scheduleDate);

    void cancelScheduledTransfer(Long transactionId);

    BigDecimal calculateTransferFee(BigDecimal amount, Currency currency);

    BigDecimal getTransferLimit(Long accountId);

    void notifyTransfer(Long transactionId);

    void validateTransfer(TransferRequest request);
}
