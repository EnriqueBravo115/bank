package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.REGISTER;
import static dev.enrique.bank.commons.constants.PathConstants.REGISTER_ACTIVATE;
import static dev.enrique.bank.commons.constants.PathConstants.REGISTER_CHECK;
import static dev.enrique.bank.commons.constants.PathConstants.REGISTER_CODE;
import static dev.enrique.bank.commons.constants.PathConstants.REGISTER_CONFIRM;

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
import dev.enrique.bank.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(REGISTER)
public class RegistrationController {
    private final RegistrationService registrationService;

    @PostMapping(REGISTER_CHECK)
    public ResponseEntity<String> registration(@Valid @RequestBody RegistrationRequest request,
            BindingResult bindingResult) {
        return ResponseEntity.ok(registrationService.registration(request, bindingResult));
    }

    @PostMapping(REGISTER_CODE)
    public ResponseEntity<String> sendRegistrationCode(@Valid @RequestBody EmailRequest request,
            BindingResult bindingResult) {
        return ResponseEntity.ok(registrationService.sendRegistrationCode(request.getEmail(), bindingResult));
    }

    @GetMapping(REGISTER_ACTIVATE)
    public ResponseEntity<String> checkRegistrationCode(@PathVariable("code") String code) {
        return ResponseEntity.ok(registrationService.checkRegistrationCode(code));
    }

    @PostMapping(REGISTER_CONFIRM)
    public ResponseEntity<AuthenticationResponse> endRegistration(@Valid @RequestBody EndRegistrationRequest request,
            BindingResult bindingResult) {
        return ResponseEntity.ok(registrationService.endRegistration(
                request.getEmail(), request.getPassword(), bindingResult));
    }
}
