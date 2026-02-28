package dev.enrique.bank.dto.response;

import dev.enrique.bank.commons.enums.Country;
import dev.enrique.bank.commons.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalInfoResponse {
    private String names;
    private String firstSurname;
    private String secondSurname;
    private String birthday;
    private Gender gender;
    private Country countryOfBirth;

    // TODO:
    // - private String fullName; names + surnames
    // - private LocalDateTime lastUpdatedPersonal;
}
