package dev.enrique.bank.dao;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import dev.enrique.bank.commons.dto.response.UserFullNameResponse;
import dev.enrique.bank.commons.dto.response.UserProfileDetailResponse;
import dev.enrique.bank.commons.dto.response.UserProfileResponse;
import dev.enrique.bank.commons.enums.RegisterStatus;
import dev.enrique.bank.model.UserProfile;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserProfileResponse(
                p.id, p.names, p.firstSurname, p.secondSurname, p.birthday, p.gender, p.countryOfBirth)
            FROM UserProfile p
            WHERE p.user.keycloakId = :keycloakId
            """)
    Optional<UserProfileResponse> getProfileByKeycloakId(String keycloakId);

    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserProfileDetailResponse(
                p.id, p.user.email, p.user.phoneNumber, p.names, p.firstSurname,
                p.secondSurname, p.birthday, p.gender, p.countryOfBirth,
                p.user.role, p.user.registerStatus, p.user.active)
            FROM UserProfile p
            WHERE p.id = :userId
            """)
    Optional<UserProfileDetailResponse> getProfileDetailByUserId(Long userId);

    @Query(value = """
            SELECT new dev.enrique.bank.commons.dto.response.UserFullNameResponse(
                p.id, p.names, p.firstSurname, p.secondSurname)
            FROM UserProfile p
            WHERE LOWER(CONCAT(p.names, ' ', p.firstSurname, ' ', COALESCE(p.secondSurname, '')))
                LIKE LOWER(CONCAT('%', :name, '%'))
            ORDER BY p.id
            """,
            countQuery = """
            SELECT COUNT(p) FROM UserProfile p
            WHERE LOWER(CONCAT(p.names, ' ', p.firstSurname, ' ', COALESCE(p.secondSurname, '')))
                LIKE LOWER(CONCAT('%', :name, '%'))
            """)
    Page<UserFullNameResponse> searchByName(String name, Pageable pageable);

    @Query(value = """
            SELECT new dev.enrique.bank.commons.dto.response.UserProfileDetailResponse(
                p.id, p.user.email, p.user.phoneNumber, p.names, p.firstSurname,
                p.secondSurname, p.birthday, p.gender, p.countryOfBirth,
                p.user.role, p.user.registerStatus, p.user.active)
            FROM UserProfile p
            WHERE p.user.registerStatus = :status
            ORDER BY p.id
            """,
            countQuery = "SELECT COUNT(p) FROM UserProfile p WHERE p.user.registerStatus = :status")
    Page<UserProfileDetailResponse> getProfilesByRegisterStatus(RegisterStatus status, Pageable pageable);
}
