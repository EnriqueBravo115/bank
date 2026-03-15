package dev.enrique.bank.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import dev.enrique.bank.commons.dto.response.UserFinancialInfoDetailResponse;
import dev.enrique.bank.commons.dto.response.UserFinancialInfoResponse;
import dev.enrique.bank.commons.enums.IncomeSource;
import dev.enrique.bank.commons.enums.MaritalStatus;
import dev.enrique.bank.commons.enums.OccupationType;
import dev.enrique.bank.commons.exception.UserNotFoundException;
import dev.enrique.bank.dao.UserFinancialInfoRepository;
import dev.enrique.bank.service.UserFinancialInfoService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserFinancialInfoServiceImpl implements UserFinancialInfoService {
    private final UserFinancialInfoRepository userFinancialInfoRepository;

    @Override
    public UserFinancialInfoResponse getFinancialInfoByUserId(Long userId) {
        return userFinancialInfoRepository.getFinancialInfoByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    public UserFinancialInfoResponse getFinancialInfoByEmail(String email) {
        return userFinancialInfoRepository.getFinancialInfoByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
    }

    @Override
    public UserFinancialInfoDetailResponse getFinancialInfoDetailByUserId(Long userId) {
        return userFinancialInfoRepository.getFinancialInfoDetailByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    public List<UserFinancialInfoResponse> getFinancialInfoByOccupationType(OccupationType occupationType) {
        return userFinancialInfoRepository.getFinancialInfoByOccupationType(occupationType);
    }

    @Override
    public List<UserFinancialInfoResponse> getFinancialInfoByIncomeSource(IncomeSource incomeSource) {
        return userFinancialInfoRepository.getFinancialInfoByIncomeSource(incomeSource);
    }

    @Override
    public List<UserFinancialInfoResponse> getFinancialInfoByMaritalStatus(MaritalStatus maritalStatus) {
        return userFinancialInfoRepository.getFinancialInfoByMaritalStatus(maritalStatus);
    }

    @Override
    public List<UserFinancialInfoResponse> getFinancialInfoByIncomeRange(BigDecimal min, BigDecimal max) {
        return userFinancialInfoRepository.getFinancialInfoByIncomeRange(min, max);
    }

    @Override
    public List<UserFinancialInfoDetailResponse> getAllFinancialInfoDetailed() {
        return userFinancialInfoRepository.getAllFinancialInfoDetailed();
    }

    @Override
    public List<UserFinancialInfoResponse> getFinancialInfoFromActiveUsers() {
        return userFinancialInfoRepository.getFinancialInfoFromActiveUsers();
    }

    @Override
    public boolean existsFinancialInfoByUserId(Long userId) {
        return userFinancialInfoRepository.existsFinancialInfoByUserId(userId);
    }
}
