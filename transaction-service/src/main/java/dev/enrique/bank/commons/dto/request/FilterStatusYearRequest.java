package dev.enrique.bank.commons.dto.request;

import dev.enrique.bank.commons.enums.TransactionStatus;

public record FilterStatusYearRequest(
        String sourceIdentifier,
        Integer year,
        TransactionStatus status) {
}