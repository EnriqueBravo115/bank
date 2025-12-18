package dev.enrique.bank.commons.dto.request;

import dev.enrique.bank.commons.enums.IdentifierType;
import dev.enrique.bank.commons.enums.TransactionStatus;

public record FilterStatusIdentifierTypeRequest(
        String sourceIdentifier,
        TransactionStatus status,
        IdentifierType identifierType) {
}
