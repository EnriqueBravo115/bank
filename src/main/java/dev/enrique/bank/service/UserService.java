package dev.enrique.bank.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import dev.enrique.bank.domain.Role;
import dev.enrique.bank.domain.User;
import dev.enrique.bank.dto.LoginDto;
import dev.enrique.bank.dto.RegisterDto;
import dev.enrique.bank.repository.projection.UserProjection;

public interface UserService {
    UserProjection getUserById(Long userId);

    Page<UserProjection> getUsers(Pageable pageable);

    Role saveRole(Role role);

    User saveUser(User user);

}
