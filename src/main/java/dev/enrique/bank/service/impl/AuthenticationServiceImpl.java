package dev.enrique.bank.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import dev.enrique.bank.commons.enums.RoleName;
import dev.enrique.bank.config.JwtProvider;
import dev.enrique.bank.domain.Role;
import dev.enrique.bank.domain.User;
import dev.enrique.bank.dto.BearerToken;
import dev.enrique.bank.dto.LoginDto;
import dev.enrique.bank.dto.RegisterDto;
import dev.enrique.bank.repository.RoleRepository;
import dev.enrique.bank.repository.UserRepository;
import dev.enrique.bank.service.AuthenticationService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtUtilities;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public String authenticate(LoginDto loginDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDto.getUsername(),
                        loginDto.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        List<String> rolesNames = new ArrayList<>();

        user.getRoles().forEach(r -> rolesNames.add(r.getRoleName()));
        String token = jwtUtilities.generateToken(user.getUsername(), rolesNames);
        return "User login successful! Token: " + token;
    }

    @Override
    public ResponseEntity<?> register(RegisterDto registerDto) {
        if (userRepository.existsByEmail(registerDto.getEmail())) {
            return new ResponseEntity<>("email is already taken !", HttpStatus.SEE_OTHER);
        } else {
            User user = new User();
            user.setEmail(registerDto.getEmail());
            user.setPassword(passwordEncoder.encode(registerDto.getPassword()));

            String myrole = "user";

            if (registerDto.getUserRole().equals("") || registerDto.getUserRole().equals("user")) {
                myrole = "USER";
            }

            if (registerDto.getUserRole().equals("admin")) {
                myrole = "ADMIN";
            }

            Role role = roleRepository.findByRoleName(RoleName.valueOf(myrole));

            user.setUserRole(registerDto.getUserRole());

            user.setRoles(Collections.singletonList(role));
            userRepository.save(user);

            String token = jwtUtilities.generateToken(registerDto.getEmail(),
                    Collections.singletonList(role.getRoleName()));
            return new ResponseEntity<>(new BearerToken(token, "Bearer"), HttpStatus.OK);
        }
    }
}
