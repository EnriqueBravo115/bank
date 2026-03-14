package dev.enrique.bank.service;

import java.util.List;

import dev.enrique.bank.commons.dto.response.UserFullNameResponse;
import dev.enrique.bank.commons.dto.response.UserProfileDetailResponse;
import dev.enrique.bank.commons.dto.response.UserProfileResponse;
import dev.enrique.bank.commons.enums.Country;
import dev.enrique.bank.commons.enums.Gender;
import dev.enrique.bank.commons.enums.RegisterStatus;

public interface UserProfileService {
    UserProfileResponse getProfileByUserId(Long userId);

    UserProfileResponse getProfileByEmail(String email);

    UserProfileDetailResponse getProfileDetailByUserId(Long userId);

    UserFullNameResponse getFullNameByUserId(Long userId);

    List<UserProfileResponse> getProfilesByGender(Gender gender);

    List<UserProfileResponse> getProfilesByCountry(Country country);

    List<UserProfileDetailResponse> getAllProfilesDetailed();

    List<UserProfileResponse> getActiveUserProfiles();

    List<UserFullNameResponse> searchByName(String name);

    List<UserProfileDetailResponse> getProfilesByRegisterStatus(RegisterStatus status);
}
