package dev.enrique.bank.config;

import static dev.enrique.bank.commons.constants.PathConstants.REGISTER;

import java.io.IOException;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import dev.enrique.bank.dao.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Runs after JWT authentication:
 * <ul>
 * <li>Rejects blocked users on every endpoint. Their access token stays valid until it expires,
 * so the token alone is not enough; the DB is checked on each request.</li>
 * <li>Outside the registration steps, requires the registration to have reached KYC.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class RegisterStatusFilter extends OncePerRequestFilter {
    private static final Set<String> COMPLETE_STATUSES = Set.of("KYC", "COMPLETE");

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Anonymous requests (POST /register) carry no JWT.
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof Jwt jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Users without a DB row (admins created in Keycloak) are governed by Keycloak alone.
        if (userRepository.existsByKeycloakIdAndActiveFalse(jwt.getSubject())) {
            sendForbiddenResponse(response, "{\"error\": \"User blocked\"}");
            return;
        }

        // Registration steps are reached by users whose registration is still in progress.
        if (isRegistrationPath(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String registrationStatus = jwt.getClaimAsString("registration_status");
        if (registrationStatus == null || !COMPLETE_STATUSES.contains(registrationStatus)) {
            sendForbiddenResponse(response, String.format(
                    "{\"error\": \"Registration incomplete\", \"status\": \"%s\"}",
                    registrationStatus != null ? registrationStatus : "UNKNOWN"));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static boolean isRegistrationPath(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals(REGISTER) || path.startsWith(REGISTER + "/");
    }

    private static void sendForbiddenResponse(HttpServletResponse response, String body) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(body);
    }
}
