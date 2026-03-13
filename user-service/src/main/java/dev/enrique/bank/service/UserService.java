package dev.enrique.bank.service;

import dev.enrique.bank.commons.dto.response.UserPrincipalResponse;

public interface UserService {
    UserPrincipalResponse getUserById(Long userId);
    UserPrincipalResponse getUserByEmail(String email);
}
