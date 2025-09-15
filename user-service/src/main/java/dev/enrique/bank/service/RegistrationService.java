package dev.enrique.bank.service;

import org.springframework.validation.BindingResult;

import dev.enrique.bank.dto.request.RegistrationRequest;
import dev.enrique.bank.dto.response.AuthenticationResponse;

public interface RegistrationService {
    String registration(RegistrationRequest request, BindingResult bindingResult);
    String sendRegistrationCode(String email, BindingResult bindingResult);
    String checkRegistrationCode(String code);
    AuthenticationResponse endRegistration(String email, String password, BindingResult bindingResult);
}
