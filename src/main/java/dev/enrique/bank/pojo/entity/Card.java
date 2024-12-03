package dev.enrique.bank.pojo.entity;

import java.time.LocalDateTime;

import dev.enrique.bank.commons.enums.CardType;
import dev.enrique.bank.commons.enums.Status;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer cardNumber;

    private String cardHolderName;

    private CardType cardType;

    private LocalDateTime creationDate;

    private String cvv;

    private Status status;
}
