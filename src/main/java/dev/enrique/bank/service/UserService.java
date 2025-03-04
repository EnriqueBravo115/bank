package dev.enrique.bank.service;

import dev.enrique.bank.model.Role;
import dev.enrique.bank.model.User;
import dev.enrique.bank.dao.projection.UserProjection;

public interface UserService {
    UserProjection getUserById(Long userId);

    //Page<UserProjection> getUsers(Pageable pageable);

    Role saveRole(Role role);

    User saveUser(User user);
}
