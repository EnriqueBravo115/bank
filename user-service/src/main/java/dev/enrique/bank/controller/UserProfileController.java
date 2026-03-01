package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.PROFILE;

import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.dto.response.FinancialProfileResponse;
import dev.enrique.bank.dto.response.PersonalInfoResponse;
import dev.enrique.bank.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(PROFILE)
@RequiredArgsConstructor
public class UserProfileController {
    private final UserProfileService userProfileService;

    //@PatchMapping("/personal-info")
    //public ResponseEntity<PersonalInfoResponse> updatePersonalInfo(
    //        @RequestBody @Valid PatchPersonalInfoRequest request) {
    //    return ResponseEntity.ok(userProfileService.updatePersonalInfo(request));
    //}

    //@PatchMapping("/employment")
    //public ResponseEntity<FinancialProfileResponse> updateEmployment(
    //        @RequestBody @Valid PatchFinancialProfileRequest request) {
    //    return ResponseEntity.ok(userProfileService.updateEmployment(request));
    //}
}
