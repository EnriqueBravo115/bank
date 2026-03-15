package dev.enrique.bank.dao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import dev.enrique.bank.commons.dto.response.UserFinancialInfoDetailResponse;
import dev.enrique.bank.commons.dto.response.UserFinancialInfoResponse;
import dev.enrique.bank.commons.enums.IncomeSource;
import dev.enrique.bank.commons.enums.MaritalStatus;
import dev.enrique.bank.commons.enums.OccupationType;
import dev.enrique.bank.model.UserFinancialInfo;

public interface UserFinancialInfoRepository extends JpaRepository<UserFinancialInfo, Long> {
    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserFinancialInfoResponse(
                f.id, f.occupationType, f.employerName, f.incomeSource, f.maritalStatus, f.monthlyIncome)
            FROM UserFinancialInfo f
            WHERE f.id = :userId
            """)
    Optional<UserFinancialInfoResponse> getFinancialInfoByUserId(Long userId);

    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserFinancialInfoResponse(
                f.id, f.occupationType, f.employerName, f.incomeSource, f.maritalStatus, f.monthlyIncome)
            FROM UserFinancialInfo f
            WHERE f.user.email = :email
            """)
    Optional<UserFinancialInfoResponse> getFinancialInfoByEmail(String email);

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

    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserFinancialInfoResponse(
                f.id, f.occupationType, f.employerName, f.incomeSource, f.maritalStatus, f.monthlyIncome)
            FROM UserFinancialInfo f
            WHERE f.occupationType = :occupationType
            """)
    List<UserFinancialInfoResponse> getFinancialInfoByOccupationType(OccupationType occupationType);

    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserFinancialInfoResponse(
                f.id, f.occupationType, f.employerName, f.incomeSource, f.maritalStatus, f.monthlyIncome)
            FROM UserFinancialInfo f
            WHERE f.incomeSource = :incomeSource
            """)
    List<UserFinancialInfoResponse> getFinancialInfoByIncomeSource(IncomeSource incomeSource);

    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserFinancialInfoResponse(
                f.id, f.occupationType, f.employerName, f.incomeSource, f.maritalStatus, f.monthlyIncome)
            FROM UserFinancialInfo f
            WHERE f.maritalStatus = :maritalStatus
            """)
    List<UserFinancialInfoResponse> getFinancialInfoByMaritalStatus(MaritalStatus maritalStatus);

    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserFinancialInfoResponse(
                f.id, f.occupationType, f.employerName, f.incomeSource, f.maritalStatus, f.monthlyIncome)
            FROM UserFinancialInfo f
            WHERE f.monthlyIncome BETWEEN :min AND :max
            """)
    List<UserFinancialInfoResponse> getFinancialInfoByIncomeRange(BigDecimal min, BigDecimal max);

    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserFinancialInfoDetailResponse(
                f.id, f.user.email, f.user.profile.names, f.user.profile.firstSurname,
                f.user.profile.secondSurname, f.occupationType, f.employerName,
                f.incomeSource, f.maritalStatus, f.monthlyIncome,
                f.user.registerStatus, f.user.active)
            FROM UserFinancialInfo f
            """)
    List<UserFinancialInfoDetailResponse> getAllFinancialInfoDetailed();

    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserFinancialInfoResponse(
                f.id, f.occupationType, f.employerName, f.incomeSource, f.maritalStatus, f.monthlyIncome)
            FROM UserFinancialInfo f
            WHERE f.user.active = true
            """)
    List<UserFinancialInfoResponse> getFinancialInfoFromActiveUsers();

    @Query("""
            SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END
            FROM UserFinancialInfo f
            WHERE f.id = :userId
            """)
    boolean existsFinancialInfoByUserId(Long userId);
}
