package dev.enrique.bank.commons.dto.response;

public record AccountUpdatedResponse(
        Long id,
        String accountNumber,
        String field,
        String updatedValue) {
}
