package dev.enrique.bank.service;

import dev.enrique.bank.commons.dto.request.UserFinancialInfoRequest;
import dev.enrique.bank.commons.dto.request.UserKycDataRequest;
import dev.enrique.bank.commons.dto.request.UserProfileRequest;
import dev.enrique.bank.commons.dto.request.UserRegisterRequest;
import dev.enrique.bank.commons.dto.response.UserRegisterResponse;

public interface RegisterService {
    UserRegisterResponse register(UserRegisterRequest request);

    void updateProfile(Long userId, UserProfileRequest request);

    void updateKycData(Long userId, UserKycDataRequest request);

    void updateFinancialInfo(Long userId, UserFinancialInfoRequest request);
}
