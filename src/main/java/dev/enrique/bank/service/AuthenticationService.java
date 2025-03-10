package dev.enrique.bank.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import dev.enrique.bank.dao.projection.UserPrincipalProjection;
import dev.enrique.bank.model.User;
import dev.enrique.bank.model.dto.request.AuthenticationRequest;

@Service
public interface AuthenticationService {
    Long getAuthenticatedUserId();

    User getAuthenticatedUser();

    UserPrincipalProjection getUserPrincipalByEmail();

    Map<String, Object> login(AuthenticationRequest request, BindingResult bindingResult);
}
