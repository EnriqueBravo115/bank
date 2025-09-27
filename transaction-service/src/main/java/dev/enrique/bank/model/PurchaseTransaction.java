package dev.enrique.bank.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Data;

@Entity
@Table(name = "purchase_transaction")
@Builder
@Data
public class PurchaseTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_card_number")
    private String sourceCardNumber;

    @Column(name = "cvv")
    private String cvv;

    @Column(name = "merchant_code")
    private String merchantCode;

    @Column(name = "merchant_category")
    private String merchantCategory;

    @Column(name = "pos_id")
    private String posId;

    @OneToOne(mappedBy = "purchaseTransaction")
    private Transaction transaction;
}
