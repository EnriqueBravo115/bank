package dev.enrique.bank.commons.dto.request;

import dev.enrique.bank.commons.enums.TransactionStatus;

public record FilterStatusKeywordRequest(
        String sourceIdentifier,
        TransactionStatus status,
        String keyword) {
}
