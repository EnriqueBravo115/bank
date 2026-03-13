package dev.enrique.bank.service;

import java.util.List;

import dev.enrique.bank.commons.dto.response.UserDetailedResponse;
import dev.enrique.bank.commons.dto.response.UserPrincipalResponse;
import dev.enrique.bank.commons.enums.UserRole;

public interface UserService {
    UserPrincipalResponse getUserById(Long userId);

    UserPrincipalResponse getUserByEmail(String email);

    UserDetailedResponse getUserByKeycloakId(String keycloakId);

    List<UserDetailedResponse> getActiveUsers();

    List<UserDetailedResponse> getUsersByRole(UserRole role);
}
