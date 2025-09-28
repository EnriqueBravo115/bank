package dev.enrique.bank.model;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.SecurityVerificationMethod;
import dev.enrique.bank.commons.enums.WithdrawalMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Data;

@Entity
@Table(name = "withdrawal_transaction")
@Builder
@Data
public class WithdrawalTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "atm_location")
    private String atmLocation;

    @Column(name = "atm_session_id")
    private String atmSessionId;

    @Column(name = "receipt_number")
    private String receiptNumber;

    @Column(name = "branch_code")
    private String branchCode;

    @Column(name = "teller_id")
    private String tellerId;

    @Column(name = "transaction_fee")
    private BigDecimal transactionFee;

    @Column(name = "security_verification_method")
    @Enumerated(EnumType.STRING)
    private SecurityVerificationMethod securityVerificationMethod;

    @Column(name = "withdrawal_method")
    @Enumerated(EnumType.STRING)
    private WithdrawalMethod withdrawalMethod;

    @OneToOne(mappedBy = "withdrawalTransaction")
    private Transaction transaction;
}
