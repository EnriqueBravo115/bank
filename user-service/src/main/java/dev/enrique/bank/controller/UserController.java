package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.ME_EMAIL;
import static dev.enrique.bank.commons.constants.PathConstants.USER;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.commons.constants.Paging;
import dev.enrique.bank.commons.dto.request.UserActiveUpdateRequest;
import dev.enrique.bank.commons.dto.request.UserEmailUpdateRequest;
import dev.enrique.bank.commons.dto.request.UserPhoneUpdateRequest;
import dev.enrique.bank.commons.dto.request.UserRoleUpdateRequest;
import dev.enrique.bank.commons.dto.response.UserDetailedResponse;
import dev.enrique.bank.commons.dto.response.UserPrincipalResponse;
import dev.enrique.bank.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    public ResponseEntity<PagedModel<UserDetailedResponse>> getActiveUsers(
            @RequestParam(defaultValue = Paging.DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = Paging.DEFAULT_SIZE) @Min(1) @Max(Paging.MAX_SIZE) int size) {
        return ResponseEntity.ok(new PagedModel<>(userService.getActiveUsers(PageRequest.of(page, size))));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{userId}/role")
    public ResponseEntity<UserDetailedResponse> updateRole(
            @PathVariable Long userId,
            @Valid @RequestBody UserRoleUpdateRequest request) {
        return ResponseEntity.ok(userService.updateRole(userId, request.getRole()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{userId}/active")
    public ResponseEntity<UserDetailedResponse> updateActive(
            @PathVariable Long userId,
            @Valid @RequestBody UserActiveUpdateRequest request) {
        return ResponseEntity.ok(userService.updateActive(userId, request.getActive()));
    }

    // Any authenticated user, also with the registration in progress: a mistyped email has to be fixed
    // before KYC, which requires it to be verified (RegisterStatusFilter lets this path through).
    @PutMapping(ME_EMAIL)
    public ResponseEntity<UserDetailedResponse> updateEmail(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserEmailUpdateRequest request) {
        return ResponseEntity.ok(userService.updateEmail(jwt.getSubject(), request.getEmail()));
    }

    @PreAuthorize("hasRole('CUSTOMER_BASIC')")
    @PutMapping("/me/phone")
    public ResponseEntity<UserDetailedResponse> updatePhone(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserPhoneUpdateRequest request) {
        return ResponseEntity.ok(userService.updatePhone(jwt.getSubject(), request.getPhoneCode(), request.getPhoneNumber()));
    }
}
