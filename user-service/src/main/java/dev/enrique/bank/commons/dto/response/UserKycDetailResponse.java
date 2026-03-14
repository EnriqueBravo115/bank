package dev.enrique.bank.commons.dto.response;

import dev.enrique.bank.commons.enums.DocumentType;
import dev.enrique.bank.commons.enums.RegisterStatus;

public record UserKycDetailResponse(
        Long id,
        String email,
        String names,
        String firstSurname,
        String secondSurname,
        String rfc,
        String curp,
        DocumentType documentType,
        RegisterStatus registerStatus) {
}
