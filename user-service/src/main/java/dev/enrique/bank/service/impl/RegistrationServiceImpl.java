package dev.enrique.bank.service.impl;

import static dev.enrique.bank.constants.ErrorMessage.ACTIVATION_CODE_NOT_FOUND;
import static dev.enrique.bank.constants.ErrorMessage.EMAIL_HAS_ALREADY_BE_TAKEN;
import static dev.enrique.bank.constants.ErrorMessage.PASSWORD_LENGTH_ERROR;
import static dev.enrique.bank.constants.ErrorMessage.USER_NOT_FOUND;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
import dev.enrique.bank.mapper.BasicMapper;
import dev.enrique.bank.model.User;
import dev.enrique.bank.service.EmailService;
import dev.enrique.bank.service.RegistrationService;
import dev.enrique.bank.service.util.UserHelper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {
    private final UserHelper userHelper;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final BasicMapper basicMapper;

    @Override
    @Transactional
    public String registration(RegistrationRequest request, BindingResult bindingResult) {
        userHelper.processInputErrors(bindingResult);
        Optional<User> existingUser = userRepository.getUserByEmail(request.getEmail(), User.class);

        if (existingUser.isEmpty()) {
            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setFullName(request.getFullName());
            user.setBirthday(request.getBirthday());
            user.setCountry(request.getCountry());
            user.setGender(request.getGender());
            user.setPhoneNumber(request.getPhoneNumber());
            user.setPhoneCode(request.getPhoneCode());
            user.setRole(UserRole.USER);
            userRepository.save(user);
            return "User data checked";
        }
        if (!existingUser.get().isActive()) {
            existingUser.get().setFullName(request.getFullName());
            existingUser.get().setUsername(request.getUsername());
            existingUser.get().setBirthday(request.getBirthday());
            userRepository.save(existingUser.get());
            return "User data checked";
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

        String token = jwtProvider.generateToken(UserRole.USER.name(), email);
        Map<String, Object> result = Map.of("user", user, "token", token);

        return AuthenticationResponse.builder()
                .user(basicMapper.convertToResponse(result.get("user"), AuthUserResponse.class))
                .token((String) result.get("token"))
                .build();

    }
}
