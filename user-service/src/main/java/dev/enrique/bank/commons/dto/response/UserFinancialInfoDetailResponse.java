package dev.enrique.bank.commons.dto.response;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.IncomeSource;
import dev.enrique.bank.commons.enums.MaritalStatus;
import dev.enrique.bank.commons.enums.OccupationType;
import dev.enrique.bank.commons.enums.RegisterStatus;

public record UserFinancialInfoDetailResponse(
        Long id,
        String email,
        String names,
        String firstSurname,
        String secondSurname,
        OccupationType occupationType,
        String employerName,
        IncomeSource incomeSource,
        MaritalStatus maritalStatus,
        BigDecimal monthlyIncome,
        RegisterStatus registerStatus,
        boolean active) {
}
