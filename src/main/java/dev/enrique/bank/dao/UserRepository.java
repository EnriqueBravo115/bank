package dev.enrique.bank.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.enrique.bank.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT user FROM User user WHERE user.id = :userId")
    <T> Optional<T> getUserById(@Param("userId") Long userId, Class<T> type);

    Optional<User> findByUsername(String username);

    // Page<UserProjection> findAllProjected(Pageable pageable);

    Boolean existsByEmail(String email);

    @Query("SELECT user FROM User user WHERE user.email = :email")
    <T> Optional<T> getUserByEmail(@Param("email") String email, Class<T> type);

}
