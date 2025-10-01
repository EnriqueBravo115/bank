package dev.enrique.bank.commons.util;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import dev.enrique.bank.client.AccountClient;
import dev.enrique.bank.commons.dto.request.ClientServiceRequest;
import dev.enrique.bank.commons.dto.request.ServiceRequest;
import dev.enrique.bank.commons.dto.response.MovementResultResponse;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.model.ServiceTransaction;
import dev.enrique.bank.model.Transaction;

@Component
public class ServiceTransactionProcessor extends AbstractTransactionProcessor<ServiceRequest, ServiceTransaction> {
    private final AccountClient accountClient;

    public ServiceTransactionProcessor(
            TransactionRepository repo,
            AccountClient accountClient,
            BasicMapper mapper) {
        super(repo, mapper);
        this.accountClient = accountClient;
    }

    @Override
    protected MovementResultResponse callAccountClient(ServiceRequest request) {
        return accountClient.processService(basicMapper.convertToResponse(request, ClientServiceRequest.class));
    }

    @Override
    protected ServiceTransaction buildSubTransaction(ServiceRequest request) {
        return ServiceTransaction.builder()
                .sourceAccountNumber(request.sourceAccountNumber())
                .paymentReference(request.paymentReference())
                .serviceType(request.serviceType())
                .build();
    }

    @Override
    protected Transaction buildTransaction(String code, ServiceRequest request, MovementResultResponse response) {
        return Transaction.builder()
                .transactionCode(code)
                .amount(request.amount())
                .description(request.description())
                .reason(response.reason())
                .transactionDate(LocalDateTime.now())
                .currency(request.currency())
                .transactionType(TransactionType.SERVICE)
                .transactionStatus(response.transactionStatus())
                .build();
    }

    @Override
    protected void link(Transaction transaction, ServiceTransaction subEntity) {
        transaction.setServiceTransaction(subEntity);
        subEntity.setTransaction(transaction);
    }
}
