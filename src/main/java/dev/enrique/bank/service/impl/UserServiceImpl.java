package dev.enrique.bank.service.impl;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import dev.enrique.bank.model.Role;
import dev.enrique.bank.model.User;
import dev.enrique.bank.commons.exception.*;
import dev.enrique.bank.dao.RoleRepository;
import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.dao.projection.UserProjection;
import dev.enrique.bank.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public UserProjection getUserById(Long userId) {
        return userRepository.getUserById(userId, UserProjection.class)
                .orElseThrow(() -> new ApiRequestException("USER NOT FOUND", HttpStatus.NOT_FOUND));
    }

    //@Override
    //public Page<UserProjection> getUsers(Pageable pageable) {
    //    return userRepository.findAllProjected(pageable);
    //}

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public Role saveRole(Role role) {
        return roleRepository.save(role);
    }

    private <T> T getUserById(Long userId, Class<T> type) {
    return userRepository.getUserById(userId, type)
            .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));
    }
}
