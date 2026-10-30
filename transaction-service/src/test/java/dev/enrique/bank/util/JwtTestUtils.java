package dev.enrique.bank.util;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class JwtTestUtils {
    private JwtTestUtils() {
    }

    // Same claims Keycloak issues: realm role (mapped to ROLE_*) and registration_status
    public static RequestPostProcessor tokenWithRole(String role) {
        return tokenWithRole(role, "COMPLETE");
    }

    public static RequestPostProcessor tokenWithRole(String role, String registrationStatus) {
        return jwt()
                .jwt(token -> token.claim("registration_status", registrationStatus))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    public static RequestPostProcessor admin() {
        return tokenWithRole("ADMIN");
    }

    public static RequestPostProcessor customer() {
        return tokenWithRole("CUSTOMER_BASIC");
    }
}
