package dev.enrique.bank.service;

import dev.enrique.bank.model.User;
import dev.enrique.bank.dao.projection.UserBasicProjection;

public interface UserService {
    UserBasicProjection getUserById(Long userId);

    //Page<UserProjection> getUsers(Pageable pageable);

    User saveUser(User user);
}
