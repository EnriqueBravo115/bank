package dev.enrique.bank.service;

import dev.enrique.bank.dao.projection.UserPrincipalProjection;
import dev.enrique.bank.dto.response.UserPrincipalResponse;

public interface UserService {
    UserPrincipalProjection getUserById(Long userId);
    UserPrincipalResponse getUserByEmail(String email);
}
