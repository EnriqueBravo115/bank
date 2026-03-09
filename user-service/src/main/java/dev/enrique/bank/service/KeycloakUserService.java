package dev.enrique.bank.service;

import dev.enrique.bank.commons.enums.UserRole;
import dev.enrique.bank.dto.request.UserProfileRequest;
import dev.enrique.bank.dto.request.UserRegisterRequest;

public interface KeycloakUserService {
    String createUser(UserRegisterRequest request);

    void updateUser(String keycloakId, UserProfileRequest request);

    void assignRole(String keycloakId, UserRole roleName);

    void updateRegistrationStatus(String keycloakId, String status);
}
