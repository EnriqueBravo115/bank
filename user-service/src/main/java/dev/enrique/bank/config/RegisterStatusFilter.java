package dev.enrique.bank.config;

import java.io.IOException;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RegisterStatusFilter extends OncePerRequestFilter {
    private static final Set<String> COMPLETE_STATUSES = Set.of("KYC", "COMPLETE");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        Jwt jwt = (Jwt) auth.getPrincipal();
        String registrationStatus = jwt.getClaimAsString("registration_status");

        if (registrationStatus == null || !COMPLETE_STATUSES.contains(registrationStatus)) {
            sendForbiddenResponse(response, registrationStatus);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendForbiddenResponse(HttpServletResponse response, String status)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(
                String.format("{\"error\": \"Registration incomplete\", \"status\": \"%s\"}",
                        status != null ? status : "UNKNOWN"));
    }
}
