package dev.enrique.bank.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import dev.enrique.bank.commons.dto.response.UserDetailedResponse;
import dev.enrique.bank.commons.dto.response.UserPrincipalResponse;
import dev.enrique.bank.commons.enums.UserRole;
import dev.enrique.bank.commons.exception.UserNotFoundException;
import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.service.UserService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public UserPrincipalResponse getUserById(Long userId) {
        return userRepository
                .getUserById(userId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    public UserPrincipalResponse getUserByEmail(String email) {
        return userRepository
                .getUserByEmail(email)
                .stream()
                .findFirst()
                .orElseThrow(() -> new UserNotFoundException(email));
    }

    @Override
    public UserDetailedResponse getUserByKeycloakId(String keycloakId) {
        return userRepository
                .getUserByKeycloakId(keycloakId)
                .orElseThrow(() -> new UserNotFoundException(keycloakId));
    }

    @Override
    public List<UserDetailedResponse> getActiveUsers() {
        return userRepository.getActiveUsers();
    }

    @Override
    public List<UserDetailedResponse> getUsersByRole(UserRole role) {
        return userRepository.getUsersByRole(role);
    }
}
