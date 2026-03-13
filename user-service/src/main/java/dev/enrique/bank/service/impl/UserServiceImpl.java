package dev.enrique.bank.service.impl;

import org.springframework.stereotype.Service;

import dev.enrique.bank.commons.exception.UserNotFoundException;
import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.dao.projection.UserPrincipalProjection;
import dev.enrique.bank.service.UserService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public UserPrincipalProjection getUserById(Long userId) {
        return userRepository
                .getUserById(userId, UserPrincipalProjection.class)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    public UserPrincipalProjection getUserByEmail(String email) {
        return userRepository
                .getUserByEmail(email, UserPrincipalProjection.class)
                .orElseThrow(() -> new UserNotFoundException(email));

    }
}
