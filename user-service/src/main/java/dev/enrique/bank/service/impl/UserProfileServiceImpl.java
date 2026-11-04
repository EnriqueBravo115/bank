package dev.enrique.bank.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;

import dev.enrique.bank.commons.dto.response.UserFullNameResponse;
import dev.enrique.bank.commons.dto.response.UserProfileDetailResponse;
import dev.enrique.bank.commons.dto.response.UserProfileResponse;
import dev.enrique.bank.commons.enums.RegisterStatus;
import dev.enrique.bank.commons.exception.UserNotFoundException;
import dev.enrique.bank.dao.UserProfileRepository;
import dev.enrique.bank.service.UserProfileService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {
    private final UserProfileRepository userProfileRepository;

    @Override
    public UserProfileResponse getProfileByKeycloakId(String keycloakId) {
        return userProfileRepository.getProfileByKeycloakId(keycloakId)
                .orElseThrow(() -> new UserNotFoundException("keycloakId", keycloakId));
    }

    @Override
    public UserProfileDetailResponse getProfileDetailByUserId(Long userId) {
        return userProfileRepository.getProfileDetailByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    public Page<UserFullNameResponse> searchByName(String name, Pageable pageable) {
        return userProfileRepository.searchByName(name, pageable);
    }

    @Override
    public Page<UserProfileDetailResponse> getProfilesByRegisterStatus(RegisterStatus status, Pageable pageable) {
        return userProfileRepository.getProfilesByRegisterStatus(status, pageable);
    }
}
