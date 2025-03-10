package dev.enrique.bank.service.impl;

import static dev.enrique.bank.commons.constants.ErrorMessage.USER_NOT_FOUND;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import dev.enrique.bank.commons.enums.UserRole;
import dev.enrique.bank.commons.exception.ApiRequestException;
import dev.enrique.bank.commons.utils.JwtProvider;
import dev.enrique.bank.commons.utils.UserServiceHelper;
import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.dao.projection.AuthUserProjection;
import dev.enrique.bank.dao.projection.UserPrincipalProjection;
import dev.enrique.bank.dto.request.AuthenticationRequest;
import dev.enrique.bank.model.User;
import dev.enrique.bank.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import static dev.enrique.bank.commons.constants.PathConstants.AUTH_USER_ID_HEADER;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
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
    public Long getAuthenticatedUserId() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAuthenticatedUserId'");
    }

    @Override
    public User getAuthenticatedUser() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAuthenticatedUser'");
    }

    @Override
    public UserPrincipalProjection getUserPrincipalByEmail() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUserPrincipalByEmail'");
    }

    private Long getUserId() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
        String userIdHeader = request.getHeader(AUTH_USER_ID_HEADER);

        if (userIdHeader == null || userIdHeader.isEmpty()) {
            throw new RuntimeException("auth user id header is missing");
        }
        return Long.parseLong(userIdHeader);
    }
}
