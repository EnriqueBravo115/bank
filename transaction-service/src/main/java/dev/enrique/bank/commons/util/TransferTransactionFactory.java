package dev.enrique.bank.commons.util;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import dev.enrique.bank.commons.dto.request.TransferRequest;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.model.Transaction;

@Component
public class TransferTransactionFactory implements TransactionFactory<TransferRequest> {
    @Override
    public Transaction build(TransferRequest request, String code, TransactionStatus transactionStatus,
            String reason) {
        return Transaction.builder()
                .amount(request.getAmount())
                .transactionCode(code)
                .sourceAccountNumber(request.getSourceAccountNumber())
                .targetAccountNumber(request.getTargetAccountNumber())
                .currency(request.getCurrency())
                .transactionType(TransactionType.TRANSFER)
                .transactionStatus(transactionStatus)
                .description(request.getDescription())
                .reason(reason)
                .transactionDate(LocalDateTime.now())
                .build();
    }

    @Override
    public Class<TransferRequest> getRequestType() {
        return TransferRequest.class;
    }
}
