package dev.enrique.bank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "transaction")
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "description")
    private String description;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "transaction_type")
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @Column(name = "transaction_status")
    @Enumerated(EnumType.STRING)
    private TransactionStatus transactionStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id")
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_account_id")
    private Account targetAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_transaction_id")
    private Transaction originalTransaction;

    @OneToMany(mappedBy = "originalTransaction", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Transaction> reversals = new ArrayList<>();
}
