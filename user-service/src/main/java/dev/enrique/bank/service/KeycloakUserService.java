package dev.enrique.bank.service;

import dev.enrique.bank.dto.request.RegisterRequest;

public interface KeycloakUserService {
    public String createUser(RegisterRequest request);
}
