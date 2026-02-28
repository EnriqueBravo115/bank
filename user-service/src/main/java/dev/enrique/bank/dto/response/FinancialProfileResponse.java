package dev.enrique.bank.dto.response;

import dev.enrique.bank.commons.enums.IncomeSource;
import dev.enrique.bank.commons.enums.MaritalStatus;
import dev.enrique.bank.commons.enums.OccupationType;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialProfileResponse {
    private OccupationType occupationType;
    private String employerName;
    private IncomeSource incomeSource;
    private MaritalStatus maritalStatus;
    private BigDecimal monthlyIncome;

    // TODO:
    // private LocalDateTime lastUpdatedFinancial;
    // private String incomeRangeDescription; "High", "Medium" (business rules)
}
