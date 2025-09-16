package dev.enrique.bank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "receipt")
public class Receipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receipt_number")
    private String receiptNumber;

    @Column(name = "issue_date")
    LocalDateTime issueDate;

    @Column(name = "value")
    private BigDecimal value;

    // Specify credit-debit
    @Column(name = "transaction_type")
    private String transactionType;

    @Column(name = "authorization_code")
    private String authorizationCode;

    @Column(name = "client_name")
    private String clientName;

    @Column(name = "rfc")
    private String rfc;

}
