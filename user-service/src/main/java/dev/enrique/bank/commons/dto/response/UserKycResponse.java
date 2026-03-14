package dev.enrique.bank.commons.dto.response;

import dev.enrique.bank.commons.enums.DocumentType;

public record UserKycResponse(
        Long id,
        String rfc,
        String curp,
        DocumentType documentType) {
}
