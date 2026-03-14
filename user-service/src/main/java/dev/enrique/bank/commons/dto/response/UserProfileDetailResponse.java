package dev.enrique.bank.commons.dto.response;

import dev.enrique.bank.commons.enums.Country;
import dev.enrique.bank.commons.enums.Gender;
import dev.enrique.bank.commons.enums.RegisterStatus;
import dev.enrique.bank.commons.enums.UserRole;

public record UserProfileDetailResponse(
        Long id,
        String email,
        String phoneNumber,
        String names,
        String firstSurname,
        String secondSurname,
        String birthday,
        Gender gender,
        Country countryOfBirth,
        UserRole role,
        RegisterStatus registerStatus,
        boolean active) {
}
