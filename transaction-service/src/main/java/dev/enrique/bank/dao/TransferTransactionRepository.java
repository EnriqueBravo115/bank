package dev.enrique.bank.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.enrique.bank.model.TransferTransaction;

public interface TransferTransactionRepository extends JpaRepository<Long, TransferTransaction> {
}
