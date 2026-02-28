package dev.enrique.bank.dto.request;

import dev.enrique.bank.commons.enums.Country;
import dev.enrique.bank.commons.enums.Gender;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PatchPersonalInfoRequest {
    @Size(max = 100, message = "Names to large")
    private String names;

    @Size(max = 100)
    private String firstSurname;

    @Size(max = 100)
    private String secondSurname;

    @Size(max = 10)
    private String birthday;

    private Gender gender;

    private Country countryOfBirth;
}
