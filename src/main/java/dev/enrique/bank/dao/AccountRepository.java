package dev.enrique.bank.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.enrique.bank.model.Account;

public interface AccountRepository extends JpaRepository<Account, Long>{
}
