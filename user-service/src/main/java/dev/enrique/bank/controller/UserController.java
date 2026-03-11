package dev.enrique.bank.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.oauth2.jwt.Jwt;

import dev.enrique.bank.dao.projection.UserPrincipalProjection;
import dev.enrique.bank.service.UserService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/user/test")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/access")
    public ResponseEntity<?> getNothing(Authentication auth) {
        Jwt jwt = (Jwt) auth.getPrincipal();
        return ResponseEntity.ok(jwt.getClaims());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-by-id/{userId}")
    public ResponseEntity<UserPrincipalProjection> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }
}
