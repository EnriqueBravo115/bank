package dev.enrique.bank.model;

import java.time.LocalDateTime;

import dev.enrique.bank.commons.enums.CardType;
import dev.enrique.bank.commons.enums.Status;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "card")
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "card_number")
    private Integer cardNumber;

    @Column(name = "card_holder_name")
    private String cardHolderName;

    @Column(name = "cvv")
    private String cvv;

    @Column(name = "card_type")
    @Enumerated(EnumType.STRING)
    private CardType cardType;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;
}
