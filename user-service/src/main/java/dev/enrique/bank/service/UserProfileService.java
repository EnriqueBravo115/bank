package dev.enrique.bank.service;

import java.util.List;

import dev.enrique.bank.commons.dto.response.UserFullNameResponse;
import dev.enrique.bank.commons.dto.response.UserProfileDetailResponse;
import dev.enrique.bank.commons.dto.response.UserProfileResponse;
import dev.enrique.bank.commons.enums.RegisterStatus;

public interface UserProfileService {
    UserProfileResponse getProfileByKeycloakId(String keycloakId);

    UserProfileDetailResponse getProfileDetailByUserId(Long userId);

    List<UserFullNameResponse> searchByName(String name);

    List<UserProfileDetailResponse> getProfilesByRegisterStatus(RegisterStatus status);
}
