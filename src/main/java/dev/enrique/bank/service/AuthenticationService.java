package dev.enrique.bank.service;

import org.springframework.http.ResponseEntity;

import dev.enrique.bank.dto.LoginDto;
import dev.enrique.bank.dto.RegisterDto;

public interface AuthenticationService {

    String authenticate(LoginDto loginDto);

    ResponseEntity<?> register(RegisterDto registerDto);
}
