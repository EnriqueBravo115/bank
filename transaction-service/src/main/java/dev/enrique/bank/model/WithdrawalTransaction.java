package dev.enrique.bank.model;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.SecurityVerificationMethod;
import dev.enrique.bank.commons.enums.WithdrawalMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "withdrawal_transaction")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalTransaction {
    @Id
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private Transaction transaction;

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
}
