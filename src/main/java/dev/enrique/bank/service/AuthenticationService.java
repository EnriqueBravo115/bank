package dev.enrique.bank.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import dev.enrique.bank.model.dto.LoginDto;
import dev.enrique.bank.model.dto.RegisterDto;

@Service
public interface AuthenticationService {
    ResponseEntity<?> authenticate(LoginDto loginDto);

    ResponseEntity<?> register(RegisterDto registerDto);
}
