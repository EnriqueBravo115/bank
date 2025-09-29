package dev.enrique.bank.commons.util;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import dev.enrique.bank.client.AccountClient;
import dev.enrique.bank.commons.dto.request.AccountPurchaseRequest;
import dev.enrique.bank.commons.dto.request.PurchaseRequest;
import dev.enrique.bank.commons.dto.response.MovementResultResponse;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.model.PurchaseTransaction;
import dev.enrique.bank.model.Transaction;

@Component
public class PurchaseTransactionProcessor extends AbstractTransactionProcessor<PurchaseRequest, PurchaseTransaction> {
    private final AccountClient accountClient;

    public PurchaseTransactionProcessor(
            TransactionRepository repo,
            AccountClient accountClient,
            BasicMapper mapper) {
        super(repo, mapper);
        this.accountClient = accountClient;
    }

    @Override
    protected MovementResultResponse callAccountClient(PurchaseRequest request) {
        return accountClient.processPurchase(basicMapper.convertToResponse(request, AccountPurchaseRequest.class));
    }

    @Override
    protected PurchaseTransaction buildSubTransaction(PurchaseRequest request) {
        return PurchaseTransaction.builder()
                .sourceCardNumber(request.sourceCardNumber())
                .cvv(request.cvv())
                .merchantCode(request.merchantCode())
                .merchantCategory(request.merchantCategory())
                .posId(request.posId())
                .build();
    }

    @Override
    protected Transaction buildTransaction(String code, PurchaseRequest request, MovementResultResponse response) {
        return Transaction.builder()
                .transactionCode(code)
                .amount(request.amount())
                .description(request.description())
                .reason(response.reason())
                .transactionDate(LocalDateTime.now())
                .currency(request.currency())
                .transactionType(TransactionType.PURCHASE)
                .transactionStatus(response.transactionStatus())
                .build();
    }

    @Override
    protected void link(Transaction transaction, PurchaseTransaction subEntity) {
        transaction.setPurchaseTransaction(subEntity);
        subEntity.setTransaction(transaction);
    }
}
