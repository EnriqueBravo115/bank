package dev.enrique.bank.service.impl;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.dao.projection.UserBasicProjection;
import dev.enrique.bank.dto.request.RegisterRequest;
import dev.enrique.bank.dto.response.RegisterResponse;
import dev.enrique.bank.commons.enums.UserRole;
import dev.enrique.bank.commons.exception.ApiRequestException;
import dev.enrique.bank.model.User;
import dev.enrique.bank.service.KeycloakUserService;
import dev.enrique.bank.service.UserService;
import dev.enrique.bank.service.util.BasicMapper;
import dev.enrique.bank.service.util.UserHelper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final KeycloakUserService keycloakUserService;
    private final UserHelper userHelper;
    private final BasicMapper basicMapper;

    @Override
    public RegisterResponse register(RegisterRequest request) {
        userHelper.ensureUserIdentifierAreUnique(request);
        String keycloakId = keycloakUserService.createUser(request);

        User user = new User();
        user.setKeycloakId(keycloakId);
        user.setEmail(request.getEmail());
        user.setNames(request.getNames());
        user.setFirstSurname(request.getFirstSurname());
        user.setSecondSurname(request.getSecondSurname());
        user.setPhoneCode(request.getPhoneCode());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setGender(request.getGender());
        user.setBirthday(request.getBirthday());
        user.setCurp(request.getCurp());
        user.setRfc(request.getRfc());
        user.setCountryOfBirth(request.getCountryOfBirth());
        user.setRole(UserRole.CUSTOMER_BASIC);
        user.setActive(false);

        userRepository.save(user);

        return basicMapper.convertToResponse(user, RegisterResponse.class);
    }

    @Override
    public UserBasicProjection getUserById(Long userId) {
        return userRepository.getUserById(userId, UserBasicProjection.class)
                .orElseThrow(() -> new ApiRequestException("USER NOT FOUND", HttpStatus.NOT_FOUND));
    }

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    private <T> T getUserById(Long userId, Class<T> type) {
        return userRepository.getUserById(userId, type)
                .orElseThrow(() -> new ApiRequestException("User not found", HttpStatus.NOT_FOUND));
    }
}
