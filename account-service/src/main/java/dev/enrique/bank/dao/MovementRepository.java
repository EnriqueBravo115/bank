package dev.enrique.bank.dao;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import dev.enrique.bank.commons.enums.MovementType;
import dev.enrique.bank.model.Movement;
import jakarta.persistence.LockModeType;

public interface MovementRepository extends JpaRepository<Movement, Long> {
    Optional<Movement> findByIdempotencyKey(String idempotencyKey);

    // COMPLETED movements of the account (as source) since the given time, except the one being
    // applied. Reversed movements do not count.
    @Query("""
            SELECT new dev.enrique.bank.dao.MovementUsage(COUNT(m), SUM(m.amount))
            FROM Movement m
            WHERE m.sourceAccountNumber = :accountNumber
            AND m.movementType = :type
            AND m.status = dev.enrique.bank.commons.enums.TransactionStatus.COMPLETED
            AND m.createdAt >= :since
            AND m.idempotencyKey <> :excludedKey
            """)
    MovementUsage usageSince(String accountNumber, MovementType type, LocalDateTime since, String excludedKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Movement m WHERE m.idempotencyKey = :idempotencyKey")
    Optional<Movement> lockByIdempotencyKey(String idempotencyKey);
}
