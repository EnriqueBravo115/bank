package dev.enrique.bank.service;

import dev.enrique.bank.commons.dto.response.UserFinancialInfoDetailResponse;
import dev.enrique.bank.commons.dto.response.UserFinancialInfoResponse;

public interface UserFinancialInfoService {
    UserFinancialInfoResponse getFinancialInfoByKeycloakId(String keycloakId);

    UserFinancialInfoDetailResponse getFinancialInfoDetailByUserId(Long userId);
}
