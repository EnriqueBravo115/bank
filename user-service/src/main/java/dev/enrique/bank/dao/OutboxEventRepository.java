package dev.enrique.bank.dao;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import dev.enrique.bank.model.OutboxEvent;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    // SKIP LOCKED lets several user-service replicas publish in parallel without sending the same row twice.
    @Query(value = """
            SELECT * FROM outbox_event
            WHERE status = 'PENDING' AND topic = :topic
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> lockPending(String topic, int limit);
}
