package dev.enrique.bank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import dev.enrique.bank.commons.enums.ScheduledTransferStatus;
import jakarta.persistence.Column;
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

    @Column(name = "quartz_job_id")
    private String quartzJobId;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "description")
    private String description;

    @Column(name = "scheduled_date")
    private LocalDateTime scheduledDate;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    @Column(name = "cancellation_date")
    private LocalDateTime cancellationDate;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ScheduledTransferStatus status;

    @ManyToOne
    private Account sourceAccount;

    @ManyToOne
    private Account targetAccount;
}
