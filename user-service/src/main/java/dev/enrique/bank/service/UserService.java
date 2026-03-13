package dev.enrique.bank.service;

import dev.enrique.bank.dao.projection.UserPrincipalProjection;
import dev.enrique.bank.commons.dto.response.UserPrincipalResponse;

public interface UserService {
    UserPrincipalProjection getUserById(Long userId);
    UserPrincipalProjection getUserByEmail(String email);
}
