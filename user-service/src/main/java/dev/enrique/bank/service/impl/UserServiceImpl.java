package dev.enrique.bank.service.impl;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.enrique.bank.broker.Topics;
import dev.enrique.bank.broker.event.UserEmailChangedEvent;
import dev.enrique.bank.broker.event.UserStatusChangedEvent;
import dev.enrique.bank.broker.outbox.EventOutbox;
import dev.enrique.bank.commons.dto.response.UserDetailedResponse;
import dev.enrique.bank.commons.dto.response.UserPrincipalResponse;
import dev.enrique.bank.commons.enums.RegisterStatus;
import dev.enrique.bank.commons.enums.UserRole;
import dev.enrique.bank.commons.exception.DuplicateUserDataException;
import dev.enrique.bank.commons.exception.EmailDeliveryException;
import dev.enrique.bank.commons.exception.InvalidRegistrationStepException;
import dev.enrique.bank.commons.exception.InvalidRoleChangeException;
import dev.enrique.bank.commons.exception.UserNotFoundException;
import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.model.User;
import dev.enrique.bank.service.KeycloakUserService;
import dev.enrique.bank.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final KeycloakUserService keycloakUserService;
    private final EventOutbox eventOutbox;

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
    public Page<UserDetailedResponse> getActiveUsers(Pageable pageable) {
        return userRepository.getActiveUsers(pageable);
    }

    @Override
    @Transactional
    public UserDetailedResponse updateRole(Long userId, UserRole role) {
        if (role == UserRole.ADMIN) {
            throw new InvalidRoleChangeException("ADMIN role cannot be granted through the API");
        }

        User user = findById(userId);

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

        return toResponse(user);
    }

    // Blocking disables the Keycloak user (no new logins, sessions ended) and freezes the account.
    @Override
    @Transactional
    public UserDetailedResponse updateActive(Long userId, boolean active) {
        User user = findById(userId);

        if (user.isActive() != active) {
            user.setActive(active);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            eventOutbox.enqueue(Topics.USER_STATUS_CHANGED, user.getKeycloakId(), new UserStatusChangedEvent(active));
            userRepository.flush();
            // If Keycloak fails, the DB change and the event are rolled back.
            keycloakUserService.setEnabled(user.getKeycloakId(), active);
        }

        return toResponse(user);
    }

    @Override
    @Transactional
    public UserDetailedResponse updateEmail(String keycloakId, String email) {
        User user = findByKeycloakId(keycloakId);

        if (user.getEmail().equalsIgnoreCase(email)) {
            return toResponse(user);
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateUserDataException("email");
        }

        user.setEmail(email);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        // account-service keeps a copy of the email on the account.
        eventOutbox.enqueue(Topics.USER_EMAIL_CHANGED, user.getKeycloakId(), new UserEmailChangedEvent(email));
        userRepository.flush();

        // Also the login username; answers 409 if Keycloak already has that email. Rolls back the DB on failure.
        keycloakUserService.updateEmail(user.getKeycloakId(), email);
        try {
            keycloakUserService.sendVerificationEmail(user.getKeycloakId());
        } catch (EmailDeliveryException ex) {
            log.warn("Verification email for user {} could not be sent: {}", keycloakId, ex.getCause().toString());
        }

        return toResponse(user);
    }

    // The phone number only lives in user_db: Keycloak does not store it.
    @Override
    @Transactional
    public UserDetailedResponse updatePhone(String keycloakId, String phoneCode, String phoneNumber) {
        User user = findByKeycloakId(keycloakId);

        if (!phoneNumber.equals(user.getPhoneNumber()) && userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new DuplicateUserDataException("phone number");
        }

        user.setPhoneCode(phoneCode);
        user.setPhoneNumber(phoneNumber);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.saveAndFlush(user);

        return toResponse(user);
    }

    private User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private User findByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UserNotFoundException("keycloakId", keycloakId));
    }

    private static UserDetailedResponse toResponse(User user) {
        return new UserDetailedResponse(
                user.getId(), user.getEmail(), user.isActive(), user.getRole(), user.getRegisterStatus());
    }
}
