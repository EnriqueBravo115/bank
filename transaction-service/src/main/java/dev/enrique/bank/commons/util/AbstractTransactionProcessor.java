package dev.enrique.bank.commons.util;

import java.util.UUID;

import dev.enrique.bank.commons.dto.response.MovementResultResponse;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.model.Transaction;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractTransactionProcessor<RQ, SUB> {
    protected final TransactionRepository transactionRepository;
    protected final BasicMapper basicMapper;

    @Transactional
    public void process(RQ request) {
        String transactionCode = UUID.randomUUID().toString();
        log.debug("Start transaction: {} for request: {}", transactionCode, request);

        try {
            MovementResultResponse response = callAccountClient(request);
            SUB subEntity = buildSubTransaction(request);

            Transaction transaction = buildTransaction(transactionCode, request, response);
            link(transaction, subEntity);

            transactionRepository.save(transaction);
            log.debug("End transaction: {}", transactionCode);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error processing transaction", e);
        }
    }

    protected abstract MovementResultResponse callAccountClient(RQ request);
    protected abstract SUB buildSubTransaction(RQ request);
    protected abstract Transaction buildTransaction(String code, RQ request, MovementResultResponse response);
    protected abstract void link(Transaction transaction, SUB subEntity);
}
