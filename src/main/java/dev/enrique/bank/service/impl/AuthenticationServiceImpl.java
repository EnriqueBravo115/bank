package dev.enrique.bank.service.impl;

import static dev.enrique.bank.commons.constants.ErrorMessage.USER_NOT_FOUND;

import java.util.Map;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import dev.enrique.bank.commons.enums.UserRole;
import dev.enrique.bank.commons.exception.ApiRequestException;
import dev.enrique.bank.commons.utils.JwtProvider;
import dev.enrique.bank.commons.utils.UserServiceHelper;
import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.dao.projection.AuthUserProjection;
import dev.enrique.bank.model.User;
import dev.enrique.bank.model.dto.RegisterDto;
import dev.enrique.bank.model.dto.request.AuthenticationRequest;
import dev.enrique.bank.service.AuthenticationService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final UserServiceHelper userServiceHelper;

    @Override
    public Map<String, Object> login(AuthenticationRequest request, BindingResult bindingResult) {
        userServiceHelper.processInputErrors(bindingResult);

        AuthUserProjection user = userRepository.getUserByEmail(request.getEmail(), AuthUserProjection.class)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));

        String token = jwtProvider.generateToken(UserRole.USER.name(), request.getEmail());
        return Map.of("user", user, "token", token);
    }

    @Override
    public ResponseEntity<?> register(RegisterDto registerDto) {
        if (userRepository.existsByEmail(registerDto.getEmail())) {
            return new ResponseEntity<>("email is already taken !", HttpStatus.SEE_OTHER);
        } else {
            User user = modelMapper.map(registerDto, User.class);
            user.setPassword(passwordEncoder.encode(registerDto.getPassword()));

            userRepository.save(user);

            return new ResponseEntity<>("ok", HttpStatus.OK);
        }
    }
}
