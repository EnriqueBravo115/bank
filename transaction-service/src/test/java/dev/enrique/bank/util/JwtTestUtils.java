package dev.enrique.bank.util;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class JwtTestUtils {
    // Subject of customer(): owner of the accounts and transactions created by the fixtures.
    public static final String OWNER = "user-1";
    public static final String OTHER_USER = "user-2";

    private JwtTestUtils() {
    }

    // Same claims Keycloak issues: subject, realm role (mapped to ROLE_*) and registration_status
    public static RequestPostProcessor token(String subject, String role, String registrationStatus) {
        return jwt()
                .jwt(token -> token.subject(subject).claim("registration_status", registrationStatus))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    public static RequestPostProcessor tokenWithRole(String role, String registrationStatus) {
        return token(OWNER, role, registrationStatus);
    }

    public static RequestPostProcessor admin() {
        return token("admin-id", "ADMIN", "COMPLETE");
    }

    public static RequestPostProcessor customer() {
        return customer(OWNER);
    }

    public static RequestPostProcessor customer(String subject) {
        return token(subject, "CUSTOMER_BASIC", "COMPLETE");
    }
}
