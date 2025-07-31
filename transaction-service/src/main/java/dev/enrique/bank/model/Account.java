package dev.enrique.bank.model;

import dev.enrique.bank.enums.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "account")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "balance")
    private BigDecimal balance;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    @Column(name = "account_type")
    @Enumerated(EnumType.STRING)
    private AccountType accountType;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "currency")
    @Enumerated(EnumType.STRING)
    private Currency currency;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Loan> loan;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Card> card;

    @OneToMany(mappedBy = "sourceAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> sourceAccount;

    @OneToMany(mappedBy = "targetAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> targetAccount;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
