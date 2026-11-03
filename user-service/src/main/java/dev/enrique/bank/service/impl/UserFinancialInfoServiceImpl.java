package dev.enrique.bank.service.impl;

import org.springframework.stereotype.Service;

import dev.enrique.bank.commons.dto.response.UserFinancialInfoDetailResponse;
import dev.enrique.bank.commons.dto.response.UserFinancialInfoResponse;
import dev.enrique.bank.commons.exception.UserNotFoundException;
import dev.enrique.bank.dao.UserFinancialInfoRepository;
import dev.enrique.bank.service.UserFinancialInfoService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserFinancialInfoServiceImpl implements UserFinancialInfoService {
    private final UserFinancialInfoRepository userFinancialInfoRepository;

    @Override
    public UserFinancialInfoResponse getFinancialInfoByKeycloakId(String keycloakId) {
        return userFinancialInfoRepository.getFinancialInfoByKeycloakId(keycloakId)
                .orElseThrow(() -> new UserNotFoundException("keycloakId", keycloakId));
    }

    @Override
    public UserFinancialInfoDetailResponse getFinancialInfoDetailByUserId(Long userId) {
        return userFinancialInfoRepository.getFinancialInfoDetailByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
