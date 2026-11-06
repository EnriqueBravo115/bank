package dev.enrique.bank.commons.util;

import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import dev.enrique.bank.commons.dto.response.MovementResultResponse;
import dev.enrique.bank.commons.dto.response.TransactionResultResponse;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Moves the money in account-service and records the transaction with the status it answered.
 * Business rejections come back as a DECLINED/FAILED {@link MovementResultResponse} (see the
 * AccountClient fallback); any other error propagates and rolls back the transaction.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractTransactionProcessor<RQ, SUB> {
    protected final TransactionRepository transactionRepository;

    @Transactional
    public TransactionResultResponse process(RQ request) {
        String transactionCode = UUID.randomUUID().toString();

        MovementResultResponse movement = callAccountClient(request);

        Transaction transaction = buildTransaction(transactionCode, request, movement);
        link(transaction, buildSubTransaction(request));
        transactionRepository.save(transaction);

        log.info("Transaction {} ({}) recorded as {}: {}", transactionCode, transaction.getTransactionType(),
                movement.transactionStatus(), movement.reason());
        return TransactionResultResponse.from(transaction);
    }

    protected abstract MovementResultResponse callAccountClient(RQ request);

    protected abstract SUB buildSubTransaction(RQ request);

    protected abstract Transaction buildTransaction(String code, RQ request, MovementResultResponse response);

    protected abstract void link(Transaction transaction, SUB subEntity);
}
