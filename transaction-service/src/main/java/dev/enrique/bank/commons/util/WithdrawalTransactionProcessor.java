package dev.enrique.bank.commons.util;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import dev.enrique.bank.client.AccountClient;
import dev.enrique.bank.commons.dto.request.AccountWithdrawalRequest;
import dev.enrique.bank.commons.dto.request.WithdrawalRequest;
import dev.enrique.bank.commons.dto.response.MovementResultResponse;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.model.Transaction;
import dev.enrique.bank.model.WithdrawalTransaction;

@Component
public class WithdrawalTransactionProcessor
        extends AbstractTransactionProcessor<WithdrawalRequest, WithdrawalTransaction> {
    private final AccountClient accountClient;

    public WithdrawalTransactionProcessor(
            TransactionRepository repo,
            AccountClient accountClient,
            BasicMapper mapper) {
        super(repo, mapper);
        this.accountClient = accountClient;
    }

    @Override
    protected MovementResultResponse callAccountClient(WithdrawalRequest request) {
        return accountClient.processWithdrawal(basicMapper.convertToResponse(request, AccountWithdrawalRequest.class));
    }

    @Override
    protected WithdrawalTransaction buildSubTransaction(WithdrawalRequest request) {
        return WithdrawalTransaction.builder()
                .atmLocation(request.atmLocation())
                .atmSessionId(request.atmSessionId())
                .receiptNumber(request.receiptNumber())
                .branchCode(request.branchCode())
                .tellerId(request.tellerId())
                .transactionFee(request.transactionFee())
                .securityVerificationMethod(request.securityVerificationMethod())
                .withdrawalMethod(request.withdrawalMethod())
                .build();
    }

    @Override
    protected Transaction buildTransaction(String code, WithdrawalRequest request, MovementResultResponse response) {
                return Transaction.builder()
                .transactionCode(code)
                .amount(request.amount())
                .description(request.description())
                .reason(response.reason())
                .transactionDate(LocalDateTime.now())
                .currency(request.currency())
                .transactionType(TransactionType.WITHDRAWAL)
                .transactionStatus(response.transactionStatus())
                .build();
    }

    @Override
    protected void link(Transaction transaction, WithdrawalTransaction subEntity) {
        transaction.setWithdrawalTransaction(subEntity);
        subEntity.setTransaction(transaction);
    }
}
