package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.USER;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.commons.dto.response.UserDetailedResponse;
import dev.enrique.bank.commons.dto.response.UserPrincipalResponse;
import dev.enrique.bank.service.UserService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(USER)
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PreAuthorize("hasRole('CUSTOMER_BASIC')")
    @GetMapping("/me")
    public ResponseEntity<UserDetailedResponse> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.getUserByKeycloakId(jwt.getSubject()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-by-id/{userId}")
    public ResponseEntity<UserPrincipalResponse> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-by-email/{email}")
    public ResponseEntity<UserPrincipalResponse> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-active")
    public ResponseEntity<List<UserDetailedResponse>> getActiveUsers() {
        return ResponseEntity.ok(userService.getActiveUsers());
    }
}
