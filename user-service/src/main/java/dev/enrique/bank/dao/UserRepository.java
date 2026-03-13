package dev.enrique.bank.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import dev.enrique.bank.commons.dto.response.UserPrincipalResponse;
import dev.enrique.bank.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    Optional<User> findByKeycloakId(String keycloakId);

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
}
