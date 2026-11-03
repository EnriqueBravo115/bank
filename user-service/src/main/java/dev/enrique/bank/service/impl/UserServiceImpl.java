package dev.enrique.bank.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.enrique.bank.commons.dto.response.UserDetailedResponse;
import dev.enrique.bank.commons.dto.response.UserPrincipalResponse;
import dev.enrique.bank.commons.enums.RegisterStatus;
import dev.enrique.bank.commons.enums.UserRole;
import dev.enrique.bank.commons.exception.InvalidRegistrationStepException;
import dev.enrique.bank.commons.exception.InvalidRoleChangeException;
import dev.enrique.bank.commons.exception.UserNotFoundException;
import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.model.User;
import dev.enrique.bank.service.KeycloakUserService;
import dev.enrique.bank.service.UserService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final KeycloakUserService keycloakUserService;

    @Override
    public UserPrincipalResponse getUserById(Long userId) {
        return userRepository.getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    public UserPrincipalResponse getUserByEmail(String email) {
        return userRepository.getUserByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("email", email));
    }

    @Override
    public UserDetailedResponse getUserByKeycloakId(String keycloakId) {
        return userRepository.getUserByKeycloakId(keycloakId)
                .orElseThrow(() -> new UserNotFoundException("keycloakId", keycloakId));
    }

    @Override
    public List<UserDetailedResponse> getActiveUsers() {
        return userRepository.getActiveUsers();
    }

    @Override
    @Transactional
    public UserDetailedResponse updateRole(Long userId, UserRole role) {
        if (role == UserRole.ADMIN) {
            throw new InvalidRoleChangeException("ADMIN role cannot be granted through the API");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // The customer role is granted at KYC; before that there is nothing to change.
        if (user.getRegisterStatus() == RegisterStatus.REGISTER || user.getRegisterStatus() == RegisterStatus.PROFILE) {
            throw new InvalidRegistrationStepException(user.getRegisterStatus(), RegisterStatus.KYC);
        }

        if (user.getRole() != role) {
            user.setRole(role);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.saveAndFlush(user);
            // Runs inside the transaction: if Keycloak fails, the DB change is rolled back.
            keycloakUserService.setCustomerTier(user.getKeycloakId(), role);
        }

        return new UserDetailedResponse(
                user.getId(), user.getEmail(), user.isActive(), user.getRole(), user.getRegisterStatus());
    }
}
