package dev.enrique.bank.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.enrique.bank.commons.enums.ScheduledTransferStatus;
import dev.enrique.bank.model.ScheduledTransfer;

public interface ScheduledTransferRepository extends JpaRepository<ScheduledTransfer, Long> {
    List<ScheduledTransfer> findByScheduledDateBeforeAndStatus(LocalDateTime date, ScheduledTransferStatus status);
}
