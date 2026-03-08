package dev.enrique.bank.config;

import java.io.IOException;

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

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // If there's no auth or it's not authenticated, let Spring Security handle it
        if (auth == null || !auth.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        String keycloakId = extractKeycloakId(auth);
        User user = userRepository.findByKeycloakId(keycloakId).orElse(null);

        // Check if the user has a valid registration phase (KYC or COMPLETED)
        if (user != null && !user.isRegistrationComplete()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\": \"Registration incomplete\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractKeycloakId(Authentication auth) {
        Jwt jwt = (Jwt) auth.getPrincipal();
        return jwt.getSubject();
    }
}
