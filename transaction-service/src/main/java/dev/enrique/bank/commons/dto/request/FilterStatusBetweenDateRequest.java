package dev.enrique.bank.commons.dto.request;

import dev.enrique.bank.commons.enums.TransactionStatus;

import java.time.LocalDateTime;

public record FilterStatusBetweenDateRequest(
        String sourceIdentifier,
        TransactionStatus status,
        LocalDateTime startDate,
        LocalDateTime endDate) {
}
