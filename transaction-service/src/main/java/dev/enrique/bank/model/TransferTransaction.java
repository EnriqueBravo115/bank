package dev.enrique.bank.model;

import dev.enrique.bank.commons.enums.IdentifierType;
import jakarta.persistence.*;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_identifier")
    private String targetIdentifier;

    @Column(name = "target_identifier_type")
    @Enumerated(EnumType.STRING)
    private IdentifierType targetIdentifierType;

    @OneToOne(mappedBy = "transferTransaction")
    private Transaction transaction;
}
