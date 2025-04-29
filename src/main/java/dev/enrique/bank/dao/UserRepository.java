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

    Boolean existsByEmail(String email);

    @Query("SELECT user FROM User user WHERE user.id = :userId")
    <T> Optional<T> getUserById(@Param("userId") Long userId, Class<T> type);

    @Query("SELECT user FROM User user WHERE user.email = :email")
    <T> Optional<T> getUserByEmail(@Param("email") String email, Class<T> type);

    @Query("SELECT user.activationCode FROM User user WHERE user.id = :userId")
    String getActivationCode(@Param("userId") Long userId);

    @Query("SELECT user FROM User user WHERE user.activationCode = :code")
    Optional<UserCommonProjection> getCommonUserByActivationCode(@Param("code") String code);

    @Query("SELECT user FROM User user WHERE user.passwordResetCode = :code")
    Optional<AuthUserProjection> getByPasswordResetCode(@Param("code") String code);

    @Modifying
    @Query("UPDATE User user SET user.activationCode = :activationCode WHERE user.id = :userId")
    void updateActivationCode(@Param("activationCode") String activationCode, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE User user SET user.password = :password WHERE user.id = :userId")
    void updatePassword(@Param("password") String password, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE User user SET user.active = true WHERE user.id = :userId")
    void updateActiveUserProfile(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE User user SET user.passwordResetCode = :passwordResetCode WHERE user.id = :userId")
    void updatePasswordResetCode(@Param("passwordResetCode") String passwordResetCode, @Param("userId") Long userId);
}
