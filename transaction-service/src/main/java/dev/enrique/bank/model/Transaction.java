package dev.enrique.bank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.IdentifierType;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transactions")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_identifier")
    private String sourceIdentifier;

    @Column(name = "identifier_type")
    @Enumerated(EnumType.STRING)
    private IdentifierType identifierType;

    @Column(name = "transaction_code")
    private String transactionCode;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "description")
    private String description;

    @Column(name = "reason")
    private String reason;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "currency")
    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Column(name = "transaction_type")
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @Column(name = "transaction_status")
    @Enumerated(EnumType.STRING)
    private TransactionStatus transactionStatus;

    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL)
    private TransferTransaction transferTransaction;

    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL)
    private PurchaseTransaction purchaseTransaction;

    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL)
    private ServiceTransaction serviceTransaction;

    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL)
    private WithdrawalTransaction withdrawalTransaction;
}
