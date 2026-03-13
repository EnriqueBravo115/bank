package dev.enrique.bank.service.impl;

import org.springframework.stereotype.Service;

import dev.enrique.bank.commons.dto.response.UserPrincipalResponse;
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
}
