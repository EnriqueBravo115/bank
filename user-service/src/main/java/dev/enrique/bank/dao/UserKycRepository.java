package dev.enrique.bank.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import dev.enrique.bank.commons.dto.response.UserKycDetailResponse;
import dev.enrique.bank.commons.dto.response.UserKycResponse;
import dev.enrique.bank.model.UserKyc;

public interface UserKycRepository extends JpaRepository<UserKyc, Long> {
    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserKycResponse(
                k.id, k.rfc, k.curp, k.documentType)
            FROM UserKyc k
            WHERE k.user.keycloakId = :keycloakId
            """)
    Optional<UserKycResponse> getKycByKeycloakId(String keycloakId);

    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserKycDetailResponse(
                k.id, k.user.email, k.user.profile.names, k.user.profile.firstSurname,
                k.user.profile.secondSurname, k.rfc, k.curp, k.documentType, k.user.registerStatus)
            FROM UserKyc k
            WHERE k.id = :userId
            """)
    Optional<UserKycDetailResponse> getKycDetailByUserId(Long userId);

    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserKycResponse(
                k.id, k.rfc, k.curp, k.documentType)
            FROM UserKyc k
            WHERE k.rfc = :rfc
            """)
    Optional<UserKycResponse> getKycByRfc(String rfc);

    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserKycResponse(
                k.id, k.rfc, k.curp, k.documentType)
            FROM UserKyc k
            WHERE k.curp = :curp
            """)
    Optional<UserKycResponse> getKycByCurp(String curp);

    boolean existsByCurpIgnoreCase(String curp);

    boolean existsByRfcIgnoreCase(String rfc);
}
