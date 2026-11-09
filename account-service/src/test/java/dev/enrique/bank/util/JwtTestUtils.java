package dev.enrique.bank.util;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class JwtTestUtils {
    private JwtTestUtils() {
    }

    public static final String CUSTOMER_NAME = "Ana Martínez";

    // Same claims Keycloak issues: subject, realm role (mapped to ROLE_*), registration_status and name
    public static RequestPostProcessor customer(String userId) {
        return jwt()
                .jwt(token -> token.subject(userId).claim("registration_status", "COMPLETE")
                        .claim("name", CUSTOMER_NAME))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER_BASIC"));
    }

    public static RequestPostProcessor admin() {
        return jwt()
                .jwt(token -> token.subject("admin-id").claim("registration_status", "COMPLETE"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    // transaction-service's client credentials token: no registration_status claim.
    public static RequestPostProcessor service() {
        return jwt()
                .jwt(token -> token.subject("service-account-transaction-service"))
                .authorities(new SimpleGrantedAuthority("ROLE_SERVICE"));
    }
}
