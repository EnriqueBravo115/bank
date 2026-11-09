package dev.enrique.bank.service;

import dev.enrique.bank.commons.enums.UserRole;
import dev.enrique.bank.commons.dto.request.UserProfileRequest;
import dev.enrique.bank.commons.dto.request.UserRegisterRequest;

public interface KeycloakUserService {
    String createUser(UserRegisterRequest request);

    void updateUser(String keycloakId, UserProfileRequest request);

    void updateName(String keycloakId, String firstName, String lastName);

    void deleteUser(String keycloakId);

    void assignRole(String keycloakId, UserRole roleName);

    void removeRole(String keycloakId, UserRole roleName);

    void setCustomerTier(String keycloakId, UserRole tier);

    void updateRegistrationStatus(String keycloakId, String status);

    void setEnabled(String keycloakId, boolean enabled);

    void updateEmail(String keycloakId, String email);

    void restoreEmail(String keycloakId, String email, boolean emailVerified);

    void sendVerificationEmail(String keycloakId);

    boolean isEmailVerified(String keycloakId);
}
