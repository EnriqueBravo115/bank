package dev.enrique.bank.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import dev.enrique.bank.commons.enums.OutboxStatus;
import dev.enrique.bank.model.OutboxEvent;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    // An event waits while the same user has an earlier one that cannot go out now (FAILED, or PENDING
    // in backoff), so a user's events are never delivered out of order. Earlier events that are ready
    // come first in the same batch, and the batch stops at the first failure.
    // SKIP LOCKED lets several user-service replicas publish in parallel without sending the same row twice;
    // it does not order a user's events across replicas.
    @Query(value = """
            SELECT * FROM outbox_event e
            WHERE e.status = 'PENDING'
              AND e.next_attempt_at <= :now
              AND NOT EXISTS (
                  SELECT 1 FROM outbox_event earlier
                  WHERE earlier.aggregate_id = e.aggregate_id
                    AND earlier.created_at < e.created_at
                    AND (earlier.status = 'FAILED'
                         OR (earlier.status = 'PENDING' AND earlier.next_attempt_at > :now)))
            ORDER BY e.created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> lockPending(LocalDateTime now, int limit);

    Page<OutboxEvent> findByStatusOrderByCreatedAt(OutboxStatus status, Pageable pageable);

    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.status = :status AND e.sentAt < :before")
    int deleteByStatusAndSentAtBefore(OutboxStatus status, LocalDateTime before);
}
