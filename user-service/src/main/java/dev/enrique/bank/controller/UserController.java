package dev.enrique.bank.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/user/test")
@RequiredArgsConstructor
public class UserController {
    @PreAuthorize("hasRole('CUSTOMER_BASIC')")
    @GetMapping("/access")
    public ResponseEntity<?> getNothing(Authentication auth) {
        return ResponseEntity.ok(auth.getAuthorities());
    }
}
