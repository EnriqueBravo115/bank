package dev.enrique.bank.service;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import dev.enrique.bank.model.dto.RegisterDto;
import dev.enrique.bank.model.dto.request.AuthenticationRequest;

@Service
public interface AuthenticationService {
    Map<String, Object> login(AuthenticationRequest request, BindingResult bindingResult);

    ResponseEntity<?> register(RegisterDto registerDto);
}
