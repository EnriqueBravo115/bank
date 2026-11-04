package dev.enrique.bank.commons.dto.response;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import dev.enrique.bank.commons.enums.Country;
import dev.enrique.bank.commons.enums.Gender;

public record UserProfileResponse(
        Long id,
        String names,
        String firstSurname,
        String secondSurname,
        @JsonFormat(pattern = "dd/MM/yyyy") LocalDate birthday,
        Gender gender,
        Country countryOfBirth) {
}
