package dev.enrique.bank.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import dev.enrique.bank.commons.dto.response.UserFullNameResponse;
import dev.enrique.bank.commons.dto.response.UserProfileDetailResponse;
import dev.enrique.bank.commons.dto.response.UserProfileResponse;
import dev.enrique.bank.commons.enums.Country;
import dev.enrique.bank.commons.enums.Gender;
import dev.enrique.bank.commons.enums.RegisterStatus;
import dev.enrique.bank.model.UserProfile;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserProfileResponse(
                p.id, p.names, p.firstSurname, p.secondSurname, p.birthday, p.gender, p.countryOfBirth)
            FROM UserProfile p
            WHERE p.id = :userId
            """)
    Optional<UserProfileResponse> getProfileByUserId(Long userId);

    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserProfileResponse(
                p.id, p.names, p.firstSurname, p.secondSurname, p.birthday, p.gender, p.countryOfBirth)
            FROM UserProfile p
            WHERE p.user.email = :email
            """)
    Optional<UserProfileResponse> getProfileByEmail(String email);

    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserProfileDetailResponse(
                p.id, p.user.email, p.user.phoneNumber, p.names, p.firstSurname,
                p.secondSurname, p.birthday, p.gender, p.countryOfBirth,
                p.user.role, p.user.registerStatus, p.user.active)
            FROM UserProfile p
            WHERE p.id = :userId
            """)
    Optional<UserProfileDetailResponse> getProfileDetailByUserId(Long userId);

    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserFullNameResponse(
                p.id, p.names, p.firstSurname, p.secondSurname)
            FROM UserProfile p
            WHERE p.id = :userId
            """)
    Optional<UserFullNameResponse> getFullNameByUserId(Long userId);

    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserProfileResponse(
                p.id, p.names, p.firstSurname, p.secondSurname, p.birthday, p.gender, p.countryOfBirth)
            FROM UserProfile p
            WHERE p.gender = :gender
            """)
    List<UserProfileResponse> getProfilesByGender(Gender gender);

    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserProfileResponse(
                p.id, p.names, p.firstSurname, p.secondSurname, p.birthday, p.gender, p.countryOfBirth)
            FROM UserProfile p
            WHERE p.countryOfBirth = :country
            """)
    List<UserProfileResponse> getProfilesByCountry(Country country);

    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserProfileDetailResponse(
                p.id, p.user.email, p.user.phoneNumber, p.names, p.firstSurname,
                p.secondSurname, p.birthday, p.gender, p.countryOfBirth,
                p.user.role, p.user.registerStatus, p.user.active)
            FROM UserProfile p
            """)
    List<UserProfileDetailResponse> getAllProfilesDetailed();

    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserProfileResponse(
                p.id, p.names, p.firstSurname, p.secondSurname, p.birthday, p.gender, p.countryOfBirth)
            FROM UserProfile p
            WHERE p.user.active = true
            """)
    List<UserProfileResponse> getActiveUserProfiles();

    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserFullNameResponse(
                p.id, p.names, p.firstSurname, p.secondSurname)
            FROM UserProfile p
            WHERE LOWER(p.names) LIKE LOWER(CONCAT('%', :name, '%'))
            """)
    List<UserFullNameResponse> searchByName(String name);

    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserProfileDetailResponse(
                p.id, p.user.email, p.user.phoneNumber, p.names, p.firstSurname,
                p.secondSurname, p.birthday, p.gender, p.countryOfBirth,
                p.user.role, p.user.registerStatus, p.user.active)
            FROM UserProfile p
            WHERE p.user.registerStatus = :status
            """)
    List<UserProfileDetailResponse> getProfilesByRegisterStatus(RegisterStatus status);
}
