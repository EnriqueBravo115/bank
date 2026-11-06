package dev.enrique.bank.commons.util;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.extern.slf4j.Slf4j;

/**
 * Undoes changes made outside the database (Keycloak) when the surrounding transaction rolls back,
 * whether the failure came from a later Keycloak call or from the commit itself.
 *
 * Register it right before the external call: the compensation must be safe to run even if that
 * call never took effect.
 */
@Slf4j
public final class RollbackCompensation {
    private RollbackCompensation() {
    }

    public static void onRollback(String description, Runnable compensation) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("No active transaction to compensate: " + description);
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_ROLLED_BACK) {
                    return;
                }
                try {
                    compensation.run();
                    log.info("Compensated after rollback: {}", description);
                } catch (RuntimeException ex) {
                    // Nothing else can be done here: leave a trace to fix it by hand.
                    log.error("Compensation failed, Keycloak and user_db are out of sync: {}", description, ex);
                }
            }
        });
    }
}
