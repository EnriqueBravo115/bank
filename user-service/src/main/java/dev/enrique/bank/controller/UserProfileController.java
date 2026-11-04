package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.PROFILE;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.commons.dto.response.UserFullNameResponse;
import dev.enrique.bank.commons.dto.response.UserProfileDetailResponse;
import dev.enrique.bank.commons.dto.response.UserProfileResponse;
import dev.enrique.bank.commons.enums.RegisterStatus;
import dev.enrique.bank.service.UserProfileService;
import dev.enrique.bank.commons.constants.Paging;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(PROFILE)
@RequiredArgsConstructor
public class UserProfileController {
    private final UserProfileService userProfileService;

    @PreAuthorize("hasRole('CUSTOMER_BASIC')")
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userProfileService.getProfileByKeycloakId(jwt.getSubject()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-detail/{userId}")
    public ResponseEntity<UserProfileDetailResponse> getProfileDetailByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(userProfileService.getProfileDetailByUserId(userId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search/{name}")
    public ResponseEntity<PagedModel<UserFullNameResponse>> searchByName(
            @PathVariable String name,
            @RequestParam(defaultValue = Paging.DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = Paging.DEFAULT_SIZE) @Min(1) @Max(Paging.MAX_SIZE) int size) {
        return ResponseEntity.ok(new PagedModel<>(userProfileService.searchByName(name, PageRequest.of(page, size))));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-by-status/{status}")
    public ResponseEntity<PagedModel<UserProfileDetailResponse>> getProfilesByRegisterStatus(
            @PathVariable RegisterStatus status,
            @RequestParam(defaultValue = Paging.DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = Paging.DEFAULT_SIZE) @Min(1) @Max(Paging.MAX_SIZE) int size) {
        return ResponseEntity.ok(new PagedModel<>(
                userProfileService.getProfilesByRegisterStatus(status, PageRequest.of(page, size))));
    }
}
