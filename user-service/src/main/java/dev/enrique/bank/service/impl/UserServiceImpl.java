package dev.enrique.bank.service.impl;

import org.springframework.stereotype.Service;

import dev.enrique.bank.commons.exception.UserNotFoundException;
import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.dao.projection.UserBasicProjection;
import dev.enrique.bank.model.User;
import dev.enrique.bank.service.KeycloakUserService;
import dev.enrique.bank.service.UserService;
import dev.enrique.bank.service.util.BasicMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final KeycloakUserService keycloakUserService;
    private final BasicMapper basicMapper;

    @Override
    public UserBasicProjection getUserById(Long userId) {
        return userRepository.getUserById(userId, UserBasicProjection.class)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    private <T> T getUserById(Long userId, Class<T> type) {
        return userRepository.getUserById(userId, type)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
