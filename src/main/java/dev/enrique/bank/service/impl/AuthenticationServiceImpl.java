package dev.enrique.bank.service.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import dev.enrique.bank.config.JwtUtil;
import dev.enrique.bank.model.User;
import dev.enrique.bank.model.dto.LoginDto;
import dev.enrique.bank.model.dto.RegisterDto;
import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.service.AuthenticationService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtilities;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Override
    public ResponseEntity<?> authenticate(LoginDto loginDto) {
        User user = userRepository.findByUsername(loginDto.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Set<String> roles = user.getRoles().stream()
                .map(r -> r.getRoleName().toString())
                .collect(Collectors.toSet());

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);

        String token = jwtUtilities.generateToken(claims, user.getUsername());

        return new ResponseEntity<>(token, HttpStatus.OK);
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
