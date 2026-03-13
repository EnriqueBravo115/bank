package dev.enrique.bank.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.commons.dto.response.UserDetailedResponse;
import dev.enrique.bank.commons.dto.response.UserPrincipalResponse;
import dev.enrique.bank.commons.enums.UserRole;
import dev.enrique.bank.service.UserService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/test-jwt")
    public ResponseEntity<?> getNothing(Authentication auth) {
        Jwt jwt = (Jwt) auth.getPrincipal();
        return ResponseEntity.ok(jwt.getClaims());
    }

    @PreAuthorize("hasRole('CUSTOMER_BASIC')")
    @GetMapping("/get-by-id/{userId}")
    public ResponseEntity<UserPrincipalResponse> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @PreAuthorize("hasRole('CUSTOMER_BASIC')")
    @GetMapping("/get-by-email/{email}")
    public ResponseEntity<UserPrincipalResponse> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @PreAuthorize("hasRole('CUSTOMER_BASIC')")
    @GetMapping("/get-by-keycloak-id/{keycloakId}")
    public ResponseEntity<UserDetailedResponse> getUserByKeycloakId(@PathVariable String keycloakId) {
        return ResponseEntity.ok(userService.getUserByKeycloakId(keycloakId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-active")
    public ResponseEntity<List<UserDetailedResponse>> getActiveUsers() {
        return ResponseEntity.ok(userService.getActiveUsers());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-by-role/{role}")
    public ResponseEntity<List<UserDetailedResponse>> getUsersByRole(@PathVariable UserRole role) {
        return ResponseEntity.ok(userService.getUsersByRole(role));
    }
}
