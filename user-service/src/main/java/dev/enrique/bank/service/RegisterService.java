package dev.enrique.bank.service;

import dev.enrique.bank.dto.request.UserRegisterRequest;
import dev.enrique.bank.dto.response.RegisterResponse;

public interface RegisterService {
    RegisterResponse register(UserRegisterRequest request);
}
