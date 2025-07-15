package dev.enrique.bank.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.dto.request.EmailRequest;
import dev.enrique.bank.dto.request.EndRegistrationRequest;
import dev.enrique.bank.dto.request.RegistrationRequest;
import dev.enrique.bank.dto.response.AuthenticationResponse;
import dev.enrique.bank.mapper.RegistrationMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/registration")
public class RegistrationController {
    private final RegistrationMapper registrationMapper;

    @PostMapping("/check")
    public ResponseEntity<String> registration(@Valid @RequestBody RegistrationRequest request,
            BindingResult bindingResult) {
        return ResponseEntity.ok(registrationMapper.registration(request, bindingResult));
    }

    @PostMapping("/code")
    public ResponseEntity<String> sendRegistrationCode(@Valid @RequestBody EmailRequest request,
            BindingResult bindingResult) {
        return ResponseEntity.ok(registrationMapper.sendRegistrationCode(request.getEmail(), bindingResult));
    }

    @GetMapping("/activate/{code}")
    public ResponseEntity<String> checkRegistrationCode(@PathVariable("code") String code) {
        return ResponseEntity.ok(registrationMapper.checkRegistrationCode(code));
    }

    @PostMapping("/confirm")
    public ResponseEntity<AuthenticationResponse> endRegistration(@Valid @RequestBody EndRegistrationRequest request,
            BindingResult bindingResult) {
        return ResponseEntity.ok(registrationMapper.endRegistration(request, bindingResult));
    }
}
