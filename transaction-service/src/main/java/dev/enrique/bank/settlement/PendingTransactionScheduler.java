package dev.enrique.bank.settlement;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

// Disabled in tests (transaction.reconciliation.enabled=false): they call retryPending themselves.
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "transaction.reconciliation.enabled", havingValue = "true", matchIfMissing = true)
public class PendingTransactionScheduler {
    private final TransactionSettlement settlement;

    @Scheduled(fixedDelayString = "${transaction.reconciliation.interval:PT5S}")
    public void retryPending() {
        settlement.retryPending();
    }
}
