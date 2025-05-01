package dev.enrique.bank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import dev.enrique.bank.commons.enums.ScheduledTransferStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ScheduledTransfer {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Account sourceAccount;

    @ManyToOne
    private Account targetAccount;

    private BigDecimal amount;
    
    private String description;

    private LocalDateTime scheduledDate;

    @Enumerated(EnumType.STRING)
    private ScheduledTransferStatus status;
    
}
