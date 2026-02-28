package dev.enrique.bank.dto.request;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.IncomeSource;
import dev.enrique.bank.commons.enums.MaritalStatus;
import dev.enrique.bank.commons.enums.OccupationType;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PatchFinancialProfileRequest {
    private OccupationType occupationType;

    @Size(max = 200)
    private String employerName;

    private IncomeSource incomeSource;

    private BigDecimal monthlyIncome;

    private MaritalStatus maritalStatus;
}
