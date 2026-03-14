package dev.enrique.bank.commons.dto.response;

public record UserFullNameResponse(
        Long id,
        String names,
        String firstSurname,
        String secondSurname) {
}
