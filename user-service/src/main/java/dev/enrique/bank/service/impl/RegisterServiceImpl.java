package dev.enrique.bank.service.impl;

import org.springframework.stereotype.Service;

import dev.enrique.bank.commons.enums.RegisterStatus;
import dev.enrique.bank.commons.enums.UserRole;
import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.dto.request.UserRegisterRequest;
import dev.enrique.bank.dto.response.RegisterResponse;
import dev.enrique.bank.model.User;
import dev.enrique.bank.service.KeycloakUserService;
import dev.enrique.bank.service.RegisterService;
import dev.enrique.bank.service.util.BasicMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegisterServiceImpl implements RegisterService {
    private final UserRepository userRepository;
    private final KeycloakUserService keycloakUserService;
    private final BasicMapper basicMapper;

    @Override
    public RegisterResponse register(UserRegisterRequest request) {
        String keycloakId = keycloakUserService.createUser(request);

        User user = new User();
        user.setKeycloakId(keycloakId);
        user.setEmail(request.getEmail());
        user.setPhoneCode(request.getPhoneCode());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(UserRole.CUSTOMER_BASIC);
        user.setRegisterStatus(RegisterStatus.CREDENTIALS);
        user.setActive(false);

        userRepository.save(user);

        return basicMapper.convertToResponse(user, RegisterResponse.class);
    }
}
