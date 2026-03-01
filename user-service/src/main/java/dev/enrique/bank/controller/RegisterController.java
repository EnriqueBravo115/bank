package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.REGISTER;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.dto.request.UserRegisterRequest;
import dev.enrique.bank.dto.response.RegisterResponse;
import dev.enrique.bank.service.RegisterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(REGISTER)
@RequiredArgsConstructor
public class RegisterController {
    private final RegisterService registerService;

    @PostMapping("/create")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        return ResponseEntity.ok(registerService.register(request));
    }

}
