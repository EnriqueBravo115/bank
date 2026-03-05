package dev.enrique.bank.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.enrique.bank.model.UserFinancialInfo;

public interface UserFinancialInfoRepository extends JpaRepository<UserFinancialInfo, Long> {

}
