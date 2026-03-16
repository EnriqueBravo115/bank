package dev.enrique.bank.model;

import dev.enrique.bank.commons.enums.IdentifierType;
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
@Table(name = "transfer_transaction")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferTransaction {
    @Id
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private Transaction transaction;

    @Column(name = "target_identifier")
    private String targetIdentifier;

    @Column(name = "target_identifier_type")
    @Enumerated(EnumType.STRING)
    private IdentifierType targetIdentifierType;
}
