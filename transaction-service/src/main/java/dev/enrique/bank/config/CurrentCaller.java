package dev.enrique.bank.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** The user behind the current request, as seen in the JWT. */
@Component
public class CurrentCaller {
    public String userId() {
        return ((Jwt) authentication().getPrincipal()).getSubject();
    }

    public boolean isAdmin() {
        return authentication().getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    /** The owner whose transactions the caller may see: null (all) for admins, themselves otherwise. */
    public String ownerScope() {
        return isAdmin() ? null : userId();
    }

    private static Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
