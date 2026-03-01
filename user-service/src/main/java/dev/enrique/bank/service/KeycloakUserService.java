package dev.enrique.bank.service;

import dev.enrique.bank.dto.request.UserRegisterRequest;

public interface KeycloakUserService {
    public String createUser(UserRegisterRequest request);
}
