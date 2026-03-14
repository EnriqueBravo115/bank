package dev.enrique.bank.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import dev.enrique.bank.commons.dto.response.UserFullNameResponse;
import dev.enrique.bank.commons.dto.response.UserProfileDetailResponse;
import dev.enrique.bank.commons.dto.response.UserProfileResponse;
import dev.enrique.bank.commons.enums.Country;
import dev.enrique.bank.commons.enums.Gender;
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
    public UserProfileResponse getProfileByUserId(Long userId) {
        return userProfileRepository.getProfileByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    public UserProfileResponse getProfileByEmail(String email) {
        return userProfileRepository.getProfileByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
    }

    @Override
    public UserProfileDetailResponse getProfileDetailByUserId(Long userId) {
        return userProfileRepository.getProfileDetailByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    public UserFullNameResponse getFullNameByUserId(Long userId) {
        return userProfileRepository.getFullNameByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    public List<UserProfileResponse> getProfilesByGender(Gender gender) {
        return userProfileRepository.getProfilesByGender(gender);
    }

    @Override
    public List<UserProfileResponse> getProfilesByCountry(Country country) {
        return userProfileRepository.getProfilesByCountry(country);
    }

    @Override
    public List<UserProfileDetailResponse> getAllProfilesDetailed() {
        return userProfileRepository.getAllProfilesDetailed();
    }

    @Override
    public List<UserProfileResponse> getActiveUserProfiles() {
        return userProfileRepository.getActiveUserProfiles();
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
