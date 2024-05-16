package dev.enrique.bank.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import dev.enrique.bank.pojo.entity.Role;
import dev.enrique.bank.pojo.entity.User;
import dev.enrique.bank.dao.projection.UserProjection;

public interface UserService {
    UserProjection getUserById(Long userId);

    Page<UserProjection> getUsers(Pageable pageable);

    Role saveRole(Role role);

    User saveUser(User user);

}
