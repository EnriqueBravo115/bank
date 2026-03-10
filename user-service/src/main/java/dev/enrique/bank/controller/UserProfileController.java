package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.PROFILE;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(PROFILE)
@RequiredArgsConstructor
public class UserProfileController {

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
