package dev.enrique.bank.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import dev.enrique.bank.commons.dto.response.UserFinancialInfoDetailResponse;
import dev.enrique.bank.commons.dto.response.UserFinancialInfoResponse;
import dev.enrique.bank.model.UserFinancialInfo;

public interface UserFinancialInfoRepository extends JpaRepository<UserFinancialInfo, Long> {
    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserFinancialInfoResponse(
                f.id, f.occupationType, f.employerName, f.incomeSource, f.maritalStatus, f.monthlyIncome)
            FROM UserFinancialInfo f
            WHERE f.user.keycloakId = :keycloakId
            """)
    Optional<UserFinancialInfoResponse> getFinancialInfoByKeycloakId(String keycloakId);

    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserFinancialInfoDetailResponse(
                f.id, f.user.email, f.user.profile.names, f.user.profile.firstSurname,
                f.user.profile.secondSurname, f.occupationType, f.employerName,
                f.incomeSource, f.maritalStatus, f.monthlyIncome,
                f.user.registerStatus, f.user.active)
            FROM UserFinancialInfo f
            WHERE f.id = :userId
            """)
    Optional<UserFinancialInfoDetailResponse> getFinancialInfoDetailByUserId(Long userId);
}
