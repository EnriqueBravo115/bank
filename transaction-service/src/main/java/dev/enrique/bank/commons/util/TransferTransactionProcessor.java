package dev.enrique.bank.commons.util;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import dev.enrique.bank.client.AccountClient;
import dev.enrique.bank.commons.dto.request.ClientTransferRequest;
import dev.enrique.bank.commons.dto.request.TransferRequest;
import dev.enrique.bank.commons.dto.response.MovementResultResponse;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.model.Transaction;
import dev.enrique.bank.model.TransferTransaction;

@Component
public class TransferTransactionProcessor extends AbstractTransactionProcessor<TransferRequest, TransferTransaction> {
    private final AccountClient accountClient;

    public TransferTransactionProcessor(
            TransactionRepository repo,
            AccountClient accountClient,
            BasicMapper mapper) {
        super(repo, mapper);
        this.accountClient = accountClient;
    }

    @Override
    protected MovementResultResponse callAccountClient(TransferRequest request) {
        return accountClient.processTransfer(basicMapper.convertToResponse(request, ClientTransferRequest.class));
    }

    @Override
    protected TransferTransaction buildSubTransaction(TransferRequest request) {
        return TransferTransaction.builder()
                .sourceAccountNumber(request.sourceAccountNumber())
                .targetAccountNumber(request.targetAccountNumber())
                .build();
    }

    @Override
    protected Transaction buildTransaction(String code, TransferRequest request, MovementResultResponse response) {
        return Transaction.builder()
                .transactionCode(code)
                .amount(request.amount())
                .description(request.description())
                .reason(response.reason())
                .transactionDate(LocalDateTime.now())
                .currency(request.currency())
                .transactionType(TransactionType.TRANSFER)
                .transactionStatus(response.transactionStatus())
                .build();
    }

    @Override
    protected void link(Transaction transaction, TransferTransaction subEntity) {
        transaction.setTransferTransaction(subEntity);
        subEntity.setTransaction(transaction);
    }
}
