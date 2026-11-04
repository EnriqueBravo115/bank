package dev.enrique.bank.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import dev.enrique.bank.commons.dto.response.UserFullNameResponse;
import dev.enrique.bank.commons.dto.response.UserProfileDetailResponse;
import dev.enrique.bank.commons.dto.response.UserProfileResponse;
import dev.enrique.bank.commons.enums.RegisterStatus;

public interface UserProfileService {
    UserProfileResponse getProfileByKeycloakId(String keycloakId);

    UserProfileDetailResponse getProfileDetailByUserId(Long userId);

    Page<UserFullNameResponse> searchByName(String name, Pageable pageable);

    Page<UserProfileDetailResponse> getProfilesByRegisterStatus(RegisterStatus status, Pageable pageable);
}
