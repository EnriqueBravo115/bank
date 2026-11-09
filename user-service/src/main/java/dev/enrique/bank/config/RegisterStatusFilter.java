package dev.enrique.bank.config;

import static dev.enrique.bank.commons.constants.PathConstants.ME_EMAIL;
import static dev.enrique.bank.commons.constants.PathConstants.REGISTER;
import static dev.enrique.bank.commons.constants.PathConstants.USER;

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
 * so the token alone is not enough; the DB is checked on each request. Only user-service does this:
 * account-service and transaction-service accept the token until it expires (5 minutes). Accepted
 * because the account is FROZEN at the same time, so no money can move, only queries.</li>
 * <li>Outside the registration steps, requires the registration to have reached KYC.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class RegisterStatusFilter extends OncePerRequestFilter {
    private static final Set<String> COMPLETE_STATUSES = Set.of("KYC", "COMPLETE");

    private final UserRepository userRepository;

    // Actuator endpoints are not customer features: SecurityConfig alone decides who reads them
    // (service accounts such as Prometheus' have no registration_status).
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.startsWith("/actuator/");
    }

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

    // The registration steps, and the email change (a mistyped email blocks the KYC step).
    private static boolean isRegistrationPath(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals(REGISTER) || path.startsWith(REGISTER + "/") || path.equals(USER + ME_EMAIL);
    }

    private static void sendForbiddenResponse(HttpServletResponse response, String body) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(body);
    }
}
