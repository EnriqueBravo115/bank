package dev.enrique.bank.event;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.model.Transaction;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionAuditService {
    private final TransactionRepository transactionRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransactionCompleted(AccountTrasferEvent event) {
        Transaction transaction = Transaction.builder()
                .amount(event.amount())
                .sourceAccount(event.sourceAccount())
                .targetAccount(event.targetAccount())
                .transactionType(TransactionType.DEPOSIT)
                .description("Transfer between accounts")
                .transactionDate(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);
    }
}
