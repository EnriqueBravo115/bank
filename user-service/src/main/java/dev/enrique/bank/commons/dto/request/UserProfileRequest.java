package dev.enrique.bank.commons.dto.request;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;

import dev.enrique.bank.commons.enums.Country;
import dev.enrique.bank.commons.enums.Gender;
import dev.enrique.bank.commons.validation.Adult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    // dd/MM/yyyy; STRICT resolving rejects impossible dates such as 31/02/1990.
    @NotNull
    @Adult
    @JsonFormat(pattern = "dd/MM/uuuu", lenient = OptBoolean.FALSE)
    private LocalDate birthday;

    @NotNull
    private Country countryOfBirth;
}
