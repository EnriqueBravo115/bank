package dev.enrique.bank.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "purchase_transaction")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseTransaction {
    @Id
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private Transaction transaction;

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
}
