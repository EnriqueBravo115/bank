package dev.enrique.bank.service;

import dev.enrique.bank.model.User;
import dev.enrique.bank.dao.projection.UserBasicProjection;
import dev.enrique.bank.dto.request.RegisterRequest;
import dev.enrique.bank.dto.response.RegisterResponse;

public interface UserService {
    RegisterResponse register(RegisterRequest request);

    UserBasicProjection getUserById(Long userId);

    // Page<UserProjection> getUsers(Pageable pageable);
    User saveUser(User user);
}
