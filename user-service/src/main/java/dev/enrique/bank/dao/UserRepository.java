package dev.enrique.bank.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.enrique.bank.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    Optional<User> findByKeycloakId(String keycloakId);

    <T> Optional<T> getUserById(Long id, Class<T> type);

    @Query("SELECT u FROM User u WHERE u.email = :email")
    <T> Optional<T> getUserByEmail(@Param("email") String email, Class<T> type);
}
