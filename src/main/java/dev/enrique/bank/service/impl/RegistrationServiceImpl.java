package dev.enrique.bank.service.impl;

import static dev.enrique.bank.commons.constants.ErrorMessage.EMAIL_HAS_ALREADY_BE_TAKEN;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import dev.enrique.bank.commons.enums.UserRole;
import dev.enrique.bank.commons.exception.ApiRequestException;
import dev.enrique.bank.commons.utils.UserServiceHelper;
import dev.enrique.bank.config.JwtProvider;
import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.dao.projection.UserCommonProjection;
import dev.enrique.bank.dto.request.RegistrationRequest;
import dev.enrique.bank.model.User;
import dev.enrique.bank.service.EmailService;
import dev.enrique.bank.service.RegistrationService;
import lombok.RequiredArgsConstructor;

import static dev.enrique.bank.commons.constants.ErrorMessage.*;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {
    private final UserRepository userRepository;
    private final UserServiceHelper userServiceHelper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final EmailService emailService;

    @Override
    @Transactional
    public String registration(RegistrationRequest request, BindingResult bindingResult) {
        userServiceHelper.processInputErrors(bindingResult);
        Optional<User> existingUser = userRepository.getUserByEmail(request.getEmail(), User.class);

        if (existingUser.isEmpty()) {
            User user = new User();
            user.setFullName(request.getFullName());
            user.setEmail(request.getEmail());
            user.setUsername(request.getUsername());
            user.setBirthday(request.getBirthday());
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
        userServiceHelper.processInputErrors(bindingResult);

        UserCommonProjection user = userRepository.getUserByEmail(email, UserCommonProjection.class)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));

        String activationCode = UUID.randomUUID().toString().substring(0, 7);
        userRepository.updateActivationCode(activationCode, user.getId());

        String subject = "Verificación de registro";
        String body = "Tu código de verificación es: " + activationCode;

        emailService.sendEmail(email, subject, body);
        return "Código de verificación enviado";
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
    public Map<String, Object> endRegistration(String email, String password, BindingResult bindingResult) {
        userServiceHelper.processInputErrors(bindingResult);
        if (password.length() < 8) {
            throw new ApiRequestException(PASSWORD_LENGTH_ERROR, HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.getUserByEmail(email, User.class)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));

        userRepository.updatePassword(passwordEncoder.encode(password), user.getId());
        userRepository.updateActiveUserProfile(user.getId());

        String token = jwtProvider.generateToken(UserRole.USER.name(), email);
        return Map.of("user", user, "token", token);
    }
}
