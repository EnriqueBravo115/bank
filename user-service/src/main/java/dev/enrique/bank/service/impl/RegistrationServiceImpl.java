package dev.enrique.bank.service.impl;

import static dev.enrique.bank.constants.ErrorMessage.ACTIVATION_CODE_NOT_FOUND;
import static dev.enrique.bank.constants.ErrorMessage.EMAIL_HAS_ALREADY_BE_TAKEN;
import static dev.enrique.bank.constants.ErrorMessage.PASSWORD_LENGTH_ERROR;
import static dev.enrique.bank.constants.ErrorMessage.USER_NOT_FOUND;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import dev.enrique.bank.configuration.JwtProvider;
import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.dao.projection.UserCommonProjection;
import dev.enrique.bank.dto.request.RegistrationRequest;
import dev.enrique.bank.dto.response.AuthUserResponse;
import dev.enrique.bank.dto.response.AuthenticationResponse;
import dev.enrique.bank.enums.UserRole;
import dev.enrique.bank.exception.ApiRequestException;
import dev.enrique.bank.exception.UniqueFieldValidationException;
import dev.enrique.bank.mapper.BasicMapper;
import dev.enrique.bank.model.User;
import dev.enrique.bank.service.EmailService;
import dev.enrique.bank.service.RegistrationService;
import dev.enrique.bank.service.util.UserHelper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final Keycloak keycloak;
    private final UserHelper userHelper;
    private final BasicMapper basicMapper;

    @Override
    @Transactional
    public String registration(RegistrationRequest request, BindingResult bindingResult) {
        userHelper.processInputErrors(bindingResult);
        userHelper.ensureUserIdentifierAreUnique(request);

        Optional<User> existingUser = userRepository.getUserByEmail(request.getEmail(), User.class);

        if (existingUser.isEmpty()) {
            User user = new User();
            user.setEmail(request.getEmail());
            user.setPhoneCode(request.getPhoneCode());
            user.setPhoneNumber(request.getPhoneNumber());
            user.setNames(request.getNames());
            user.setFirstSurname(request.getFirstSurname());
            user.setSecondSurname(request.getSecondSurname());
            user.setGender(request.getGender());
            user.setBirthday(request.getBirthday());
            user.setCountryOfBirth(request.getCountryOfBirth());
            user.setCurp(request.getCurp());
            user.setRfc(request.getRfc());
            user.setRole(UserRole.CUSTOMER_BASIC);
            userRepository.save(user);
            return "User registered";
        }
        if (!existingUser.get().isActive()) {
            existingUser.get().setPhoneCode(request.getPhoneCode());
            existingUser.get().setPhoneNumber(request.getPhoneNumber());
            existingUser.get().setNames(request.getNames());
            existingUser.get().setFirstSurname(request.getFirstSurname());
            existingUser.get().setSecondSurname(request.getSecondSurname());
            existingUser.get().setGender(request.getGender());
            existingUser.get().setBirthday(request.getBirthday());
            existingUser.get().setCountryOfBirth(request.getCountryOfBirth());
            existingUser.get().setCurp(request.getCurp());
            existingUser.get().setRfc(request.getRfc());
            userRepository.save(existingUser.get());
            return "User updated";
        }
        throw new ApiRequestException(EMAIL_HAS_ALREADY_BE_TAKEN, HttpStatus.FORBIDDEN);
    }

    @Override
    @Transactional
    public String sendRegistrationCode(String email, BindingResult bindingResult) {
        userHelper.processInputErrors(bindingResult);

        UserCommonProjection user = userRepository.getUserByEmail(email, UserCommonProjection.class)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));

        String activationCode = UUID.randomUUID().toString().substring(0, 7);
        userRepository.updateActivationCode(activationCode, user.getId());

        String subject = "Register verification";
        String body = "Verification code: " + activationCode;

        emailService.sendEmail(email, subject, body);
        return "Verification code send!";
    }

    @Override
    @Transactional
    public String checkRegistrationCode(String code) {
        UserCommonProjection user = userRepository.getCommonUserByActivationCode(code)
                .orElseThrow(() -> new ApiRequestException(ACTIVATION_CODE_NOT_FOUND, HttpStatus.NOT_FOUND));
        userRepository.updateActivationCode(null, user.getId());
        return "User succesfully updated";
    }

    @Override
    @Transactional
    public AuthenticationResponse endRegistration(String email, String password, BindingResult bindingResult) {
        userHelper.processInputErrors(bindingResult);
        if (password.length() < 8)
            throw new ApiRequestException(PASSWORD_LENGTH_ERROR, HttpStatus.BAD_REQUEST);

        User user = userRepository.getUserByEmail(email, User.class)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));

        userRepository.updatePassword(passwordEncoder.encode(password), user.getId());
        userRepository.updateActiveUserProfile(user.getId());

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);

        Map<String, Object> result = Map.of("user", user, "token", token);

        return AuthenticationResponse.builder()
                .user(basicMapper.convertToResponse(result.get("user"), AuthUserResponse.class))
                .token((String) result.get("token"))
                .build();
    }
}
