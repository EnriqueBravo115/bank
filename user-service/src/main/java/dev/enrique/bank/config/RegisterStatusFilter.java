package dev.enrique.bank.config;

import java.io.IOException;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RegisterStatusFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    private static final List<String> REGISTRATION_WHITELIST = List.of(
            "/api/v1/user/register",
            "/api/v1/user/profile",
            "/api/v1/user/kyc");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || isWhitelisted(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String keycloakId = extractKeycloakId(auth);
        User user = userRepository.findByKeycloakId(keycloakId).orElse(null);

        if (user != null && !user.isRegistrationComplete()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"Registration incomplete\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isWhitelisted(String path) {
        return REGISTRATION_WHITELIST.stream()
                .anyMatch(path::startsWith);
    }

    private String extractKeycloakId(Authentication auth) {
        Jwt jwt = (Jwt) auth.getPrincipal();
        return jwt.getSubject();
    }
}
