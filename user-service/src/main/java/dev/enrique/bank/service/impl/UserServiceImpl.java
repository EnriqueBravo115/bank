package dev.enrique.bank.service.impl;

import static dev.enrique.bank.constants.ErrorMessage.USER_NOT_FOUND;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.dao.projection.UserBasicProjection;
import dev.enrique.bank.exception.ApiRequestException;
import dev.enrique.bank.model.User;
import dev.enrique.bank.service.UserService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public UserBasicProjection getUserById(Long userId) {
        return userRepository.getUserById(userId, UserBasicProjection.class)
                .orElseThrow(() -> new ApiRequestException("USER NOT FOUND", HttpStatus.NOT_FOUND));
    }

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    private <T> T getUserById(Long userId, Class<T> type) {
    return userRepository.getUserById(userId, type)
            .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));
    }
}
