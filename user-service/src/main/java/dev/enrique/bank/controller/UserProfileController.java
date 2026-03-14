package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.PROFILE;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.commons.dto.response.UserFullNameResponse;
import dev.enrique.bank.commons.dto.response.UserProfileDetailResponse;
import dev.enrique.bank.commons.dto.response.UserProfileResponse;
import dev.enrique.bank.commons.enums.Country;
import dev.enrique.bank.commons.enums.Gender;
import dev.enrique.bank.commons.enums.RegisterStatus;
import dev.enrique.bank.service.UserProfileService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(PROFILE)
@RequiredArgsConstructor
public class UserProfileController {
    private final UserProfileService userProfileService;

    @PreAuthorize("hasRole('CUSTOMER_BASIC')")
    @GetMapping("/get-by-user-id/{userId}")
    public ResponseEntity<UserProfileResponse> getProfileByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(userProfileService.getProfileByUserId(userId));
    }

    @PreAuthorize("hasRole('CUSTOMER_BASIC')")
    @GetMapping("/get-by-email/{email}")
    public ResponseEntity<UserProfileResponse> getProfileByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userProfileService.getProfileByEmail(email));
    }

    @PreAuthorize("hasRole('CUSTOMER_BASIC')")
    @GetMapping("/get-detail/{userId}")
    public ResponseEntity<UserProfileDetailResponse> getProfileDetailByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(userProfileService.getProfileDetailByUserId(userId));
    }

    @PreAuthorize("hasRole('CUSTOMER_BASIC')")
    @GetMapping("/get-full-name/{userId}")
    public ResponseEntity<UserFullNameResponse> getFullNameByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(userProfileService.getFullNameByUserId(userId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-by-gender/{gender}")
    public ResponseEntity<List<UserProfileResponse>> getProfilesByGender(@PathVariable Gender gender) {
        return ResponseEntity.ok(userProfileService.getProfilesByGender(gender));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-by-country/{country}")
    public ResponseEntity<List<UserProfileResponse>> getProfilesByCountry(@PathVariable Country country) {
        return ResponseEntity.ok(userProfileService.getProfilesByCountry(country));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-all-detailed")
    public ResponseEntity<List<UserProfileDetailResponse>> getAllProfilesDetailed() {
        return ResponseEntity.ok(userProfileService.getAllProfilesDetailed());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-active")
    public ResponseEntity<List<UserProfileResponse>> getActiveUserProfiles() {
        return ResponseEntity.ok(userProfileService.getActiveUserProfiles());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search/{name}")
    public ResponseEntity<List<UserFullNameResponse>> searchByName(@PathVariable String name) {
        return ResponseEntity.ok(userProfileService.searchByName(name));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-by-status/{status}")
    public ResponseEntity<List<UserProfileDetailResponse>> getProfilesByRegisterStatus(
            @PathVariable RegisterStatus status) {
        return ResponseEntity.ok(userProfileService.getProfilesByRegisterStatus(status));
    }
}
