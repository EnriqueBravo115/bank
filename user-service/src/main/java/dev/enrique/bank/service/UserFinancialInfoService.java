package dev.enrique.bank.service;

import java.math.BigDecimal;
import java.util.List;

import dev.enrique.bank.commons.dto.response.UserFinancialInfoDetailResponse;
import dev.enrique.bank.commons.dto.response.UserFinancialInfoResponse;
import dev.enrique.bank.commons.enums.IncomeSource;
import dev.enrique.bank.commons.enums.MaritalStatus;
import dev.enrique.bank.commons.enums.OccupationType;

public interface UserFinancialInfoService {
    UserFinancialInfoResponse getFinancialInfoByUserId(Long userId);

    UserFinancialInfoResponse getFinancialInfoByEmail(String email);

    UserFinancialInfoDetailResponse getFinancialInfoDetailByUserId(Long userId);

    List<UserFinancialInfoResponse> getFinancialInfoByOccupationType(OccupationType occupationType);

    List<UserFinancialInfoResponse> getFinancialInfoByIncomeSource(IncomeSource incomeSource);

    List<UserFinancialInfoResponse> getFinancialInfoByMaritalStatus(MaritalStatus maritalStatus);

    List<UserFinancialInfoResponse> getFinancialInfoByIncomeRange(BigDecimal min, BigDecimal max);

    List<UserFinancialInfoDetailResponse> getAllFinancialInfoDetailed();

    List<UserFinancialInfoResponse> getFinancialInfoFromActiveUsers();

    boolean existsFinancialInfoByUserId(Long userId);
}
