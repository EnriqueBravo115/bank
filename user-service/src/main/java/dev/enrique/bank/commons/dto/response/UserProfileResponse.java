package dev.enrique.bank.commons.dto.response;

import dev.enrique.bank.commons.enums.Country;
import dev.enrique.bank.commons.enums.Gender;

public record UserProfileResponse(
        Long id,
        String names,
        String firstSurname,
        String secondSurname,
        String birthday,
        Gender gender,
        Country countryOfBirth) {
}
