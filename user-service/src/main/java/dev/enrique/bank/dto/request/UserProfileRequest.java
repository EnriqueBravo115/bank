package dev.enrique.bank.dto.request;

import dev.enrique.bank.commons.enums.Country;
import dev.enrique.bank.commons.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserProfileRequest {
    @NotBlank
    private String names;

    @NotBlank
    private String firstSurname;

    @NotBlank
    private String secondSurname;

    @NotNull
    private Gender gender;

    @Pattern(regexp = "^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/(19[2-9][0-9]|20[0-9]{2})$")
    private String birthday;

    @NotNull
    private Country countryOfBirth;
}
