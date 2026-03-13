package dev.enrique.bank.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import dev.enrique.bank.commons.dto.response.UserDetailedResponse;
import dev.enrique.bank.commons.dto.response.UserPrincipalResponse;
import dev.enrique.bank.commons.enums.UserRole;
import dev.enrique.bank.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserPrincipalResponse(u.id, u.email)
            FROM User u
            WHERE u.id = :id
            """)
    Optional<UserPrincipalResponse> getUserById(Long id);

    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserPrincipalResponse(u.id, u.email)
            FROM User u
            WHERE u.email = :email
            """)
    Optional<UserPrincipalResponse> getUserByEmail(String email);

    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserDetailedResponse(
                u.id, u.email, u.active, u.role, u.registerStatus)
            FROM User u
            WHERE u.keycloakId = :keycloakId
            """)
    Optional<UserDetailedResponse> getUserByKeycloakId(String keycloakId);

    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserDetailedResponse(
                u.id, u.email, u.active, u.role, u.registerStatus)
            FROM User u
            WHERE u.active = true
            """)
    List<UserDetailedResponse> getActiveUsers();

    @Query("""
            SELECT new dev.enrique.bank.commons.dto.response.UserDetailedResponse(
                u.id, u.email, u.active, u.role, u.registerStatus)
            FROM User u
            WHERE u.role = :role
            """)
    List<UserDetailedResponse> getUsersByRole(UserRole role);
}
