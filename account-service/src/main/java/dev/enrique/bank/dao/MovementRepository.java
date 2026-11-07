package dev.enrique.bank.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import dev.enrique.bank.model.Movement;
import jakarta.persistence.LockModeType;

public interface MovementRepository extends JpaRepository<Movement, Long> {
    Optional<Movement> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Movement m WHERE m.idempotencyKey = :idempotencyKey")
    Optional<Movement> lockByIdempotencyKey(String idempotencyKey);
}
