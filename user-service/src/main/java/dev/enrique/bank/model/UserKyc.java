package dev.enrique.bank.model;

import dev.enrique.bank.commons.enums.DocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_kyc")
@Getter
@Setter
public class UserKyc {
    @Id
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private User user;

    @Column(name = "rfc", nullable = false, unique = true, length = 13)
    private String rfc;

    @Column(name = "curp", nullable = false, unique = true, length = 18)
    private String curp;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 2)
    private DocumentType documentType;

    // @Enumerated(EnumType.STRING)
    // private KycStatus status = KycStatus.PENDING;
}
