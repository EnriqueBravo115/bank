package dev.enrique.bank.service;

import java.util.Map;

import org.springframework.validation.BindingResult;

import dev.enrique.bank.dto.request.RegistrationRequest;


public interface RegistrationService {
    String registration(RegistrationRequest request, BindingResult bindingResult);

    String sendRegistrationCode(String email, BindingResult bindingResult);

    String checkRegistrationCode(String code);

    Map<String, Object> endRegistration(String email, String password, BindingResult bindingResult);
}
