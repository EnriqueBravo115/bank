package dev.enrique.bank.mapper;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;

import dev.enrique.bank.dto.request.EndRegistrationRequest;
import dev.enrique.bank.dto.request.RegistrationRequest;
import dev.enrique.bank.dto.response.AuthUserResponse;
import dev.enrique.bank.dto.response.AuthenticationResponse;
import dev.enrique.bank.service.RegistrationService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RegistrationMapper {
    private final BasicMapper basicMapper;
    private final RegistrationService registrationService;

    public String registration(RegistrationRequest registrationRequest, BindingResult bindingResult) {
        return registrationService.registration(registrationRequest, bindingResult);
    }

    public String sendRegistrationCode(String email, BindingResult bindingResult) {
        return registrationService.sendRegistrationCode(email, bindingResult);
    }

    public String checkRegistrationCode(String code) {
        return registrationService.checkRegistrationCode(code);
    }

    public AuthenticationResponse endRegistration(EndRegistrationRequest request, BindingResult bindingResult) {
        Map<String, Object> result = registrationService
                .endRegistration(request.getEmail(), request.getPassword(), bindingResult);
        AuthenticationResponse response = new AuthenticationResponse();

        response.setUser(basicMapper.convertToResponse(result.get("user"), AuthUserResponse.class));
        response.setToken((String) result.get("token"));
        return response;
    }
}
