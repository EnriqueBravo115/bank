package dev.enrique.bank.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.enrique.bank.model.PendingUserUpdate;

public interface PendingUserUpdateRepository extends JpaRepository<PendingUserUpdate, String> {
}
