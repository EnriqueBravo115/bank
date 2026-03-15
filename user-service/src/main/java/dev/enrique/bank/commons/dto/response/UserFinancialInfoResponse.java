package dev.enrique.bank.commons.dto.response;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.IncomeSource;
import dev.enrique.bank.commons.enums.MaritalStatus;
import dev.enrique.bank.commons.enums.OccupationType;

public record UserFinancialInfoResponse(
        Long id,
        OccupationType occupationType,
        String employerName,
        IncomeSource incomeSource,
        MaritalStatus maritalStatus,
        BigDecimal monthlyIncome) {
}
