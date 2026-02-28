package dev.enrique.bank.model;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.IncomeSource;
import dev.enrique.bank.commons.enums.MaritalStatus;
import dev.enrique.bank.commons.enums.OccupationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_financial_info")
@Getter
@Setter
public class UserFinancialInfo {
    @Id
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "occupation_type")
    private OccupationType occupationType;

    @Column(name = "employer_name")
    private String employerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "income_source")
    private IncomeSource incomeSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "marital_status")
    private MaritalStatus maritalStatus;

    @Column(name = "monthly_income", precision = 12, scale = 2)
    private BigDecimal monthlyIncome;
}
