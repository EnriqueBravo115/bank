package dev.enrique.bank.service;

import org.springframework.http.ResponseEntity;

import dev.enrique.bank.pojo.dto.LoginDto;
import dev.enrique.bank.pojo.dto.RegisterDto;

public interface AuthenticationService {
    String authenticate(LoginDto loginDto);

    ResponseEntity<?> register(RegisterDto registerDto);
}
