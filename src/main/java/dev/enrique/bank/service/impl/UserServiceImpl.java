package dev.enrique.bank.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import dev.enrique.bank.domain.Role;
import dev.enrique.bank.domain.User;
import dev.enrique.bank.commons.exception.*;
import dev.enrique.bank.repository.RoleRepository;
import dev.enrique.bank.repository.UserRepository;
import dev.enrique.bank.repository.projection.UserProjection;
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

    @Override
    public Page<UserProjection> getUsers(Pageable pageable) {
        return userRepository.findAllProjected(pageable);
    }

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public Role saveRole(Role role) {
        return roleRepository.save(role);
    }
}
