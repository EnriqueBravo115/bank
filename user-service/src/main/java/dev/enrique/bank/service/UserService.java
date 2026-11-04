package dev.enrique.bank.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import dev.enrique.bank.commons.dto.response.UserDetailedResponse;
import dev.enrique.bank.commons.dto.response.UserPrincipalResponse;
import dev.enrique.bank.commons.enums.UserRole;

public interface UserService {
    UserPrincipalResponse getUserById(Long userId);

    UserPrincipalResponse getUserByEmail(String email);

    UserDetailedResponse getUserByKeycloakId(String keycloakId);

    Page<UserDetailedResponse> getActiveUsers(Pageable pageable);

    UserDetailedResponse updateRole(Long userId, UserRole role);

    UserDetailedResponse updateActive(Long userId, boolean active);

    UserDetailedResponse updateEmail(String keycloakId, String email);

    UserDetailedResponse updatePhone(String keycloakId, String phoneCode, String phoneNumber);
}
