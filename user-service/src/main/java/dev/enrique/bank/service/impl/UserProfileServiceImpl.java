package dev.enrique.bank.service.impl;

import java.util.List;

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
    public List<UserFullNameResponse> searchByName(String name) {
        return userProfileRepository.searchByName(name);
    }

    @Override
    public List<UserProfileDetailResponse> getProfilesByRegisterStatus(RegisterStatus status) {
        return userProfileRepository.getProfilesByRegisterStatus(status);
    }
}
