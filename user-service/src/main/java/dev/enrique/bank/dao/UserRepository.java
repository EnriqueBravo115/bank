package dev.enrique.bank.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.enrique.bank.dao.projection.AuthUserProjection;
import dev.enrique.bank.dao.projection.UserCommonProjection;
import dev.enrique.bank.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    @Query("""
            SELECT u FROM User u
            WHERE u.active = true
            AND (u.email = :email OR u.rfc = :rfc OR u.curp = :curp)
            """)
    Optional<User> findActiveByAnyIdentifier(@Param("email") String email,
            @Param("rfc") String rfc,
            @Param("curp") String curp);

    @Query("SELECT u FROM User u WHERE u.id = :userId")
    <T> Optional<T> getUserById(@Param("userId") Long userId, Class<T> type);

    @Query("SELECT u FROM User u WHERE u.email = :email")
    <T> Optional<T> getUserByEmail(@Param("email") String email, Class<T> type);

    @Query("SELECT u.activationCode FROM User u WHERE user.id = :userId")
    String getActivationCode(@Param("userId") Long userId);

    @Query("SELECT u FROM User u WHERE u.activationCode = :code")
    Optional<UserCommonProjection> getCommonUserByActivationCode(@Param("code") String code);

    @Query("SELECT u FROM User u WHERE u.passwordResetCode = :code")
    Optional<AuthUserProjection> getByPasswordResetCode(@Param("code") String code);

    @Query("SELECT u.password FROM User u WHERE u.id = :userId")
    String getUserPasswordById(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE User u SET u.activationCode = :activationCode WHERE u.id = :userId")
    void updateActivationCode(@Param("activationCode") String activationCode, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE User u SET u.password = :password WHERE u.id = :userId")
    void updatePassword(@Param("password") String password, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE User u SET u.active = true WHERE u.id = :userId")
    void updateActiveUserProfile(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE User u SET u.passwordResetCode = :passwordResetCode WHERE u.id = :userId")
    void updatePasswordResetCode(@Param("passwordResetCode") String passwordResetCode, @Param("userId") Long userId);
}
