package dev.enrique.bank.pojo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import dev.enrique.bank.commons.enums.LoanStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private BigDecimal loanAmount;

    private BigDecimal interestRate;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private LoanStatus status;

    private List<Repayment> repaymentSchedule;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;
}
