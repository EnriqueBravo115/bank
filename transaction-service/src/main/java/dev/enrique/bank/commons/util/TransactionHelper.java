package dev.enrique.bank.commons.util;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.dto.request.TransferRequest;
import dev.enrique.bank.model.Transaction;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TransactionHelper {
    public Transaction buildTransfer(TransferRequest transferRequest, String transactionCode,
            TransactionStatus transactionStatus, String reason) {

        return Transaction.builder()
                .amount(transferRequest.amount())
                .transactionCode(transactionCode)
                .sourceAccountNumber(transferRequest.sourceAccountNumber())
                .targetAccountNumber(transferRequest.targetAccountNumber())
                .currency(transferRequest.currency())
                .transactionType(TransactionType.TRANSFER)
                .transactionStatus(transactionStatus)
                .description(transferRequest.description())
                .reason(reason)
                .transactionDate(LocalDateTime.now())
                .build();
    }

    public void validateTranferRequest(TransferRequest transferRequest){
    }
}
