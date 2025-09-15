package dev.enrique.bank.dto.request;

import dev.enrique.bank.commons.enums.Country;
import dev.enrique.bank.commons.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationRequest {
    @NotBlank
    @Email(message = "Please enter a valid email address.")
    private String email;

    @Pattern(regexp = "^(\\+52|\\+1)$", message = "Only +52 and +1 codes are accepted")
    private String phoneCode;

    @Pattern(regexp = "^\\d{10}$", message = "Invalid phone format")
    private String phoneNumber;

    @NotBlank
    @Pattern(regexp = "^[\\p{L} ']+$", message = "Letters and spaces only")
    private String names;

    @NotBlank
    @Pattern(regexp = "^[\\p{L} ']+$", message = "Letters and spaces only")
    private String firstSurname;

    @NotBlank
    @Pattern(regexp = "^[\\p{L} ']+$", message = "Letters and spaces only")
    private String secondSurname;

    @NotBlank
    private Gender gender;

    @NotBlank
    @Pattern(regexp = "^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/(19[2-9][0-9]|20[0-9]{2})$", message = "Invalid date format")
    private String birthday;

    @NotBlank
    private Country countryOfBirth;

    @NotBlank
    @Size(min = 18, max = 18, message = "CURP must be exactly 18 characters long")
    @Pattern(regexp = "^[A-Z]{4}[0-9]{6}[HM][A-Z]{5}[0-9A-Z]{2}$", message = "Invalid CURP format")
    private String curp;

    @NotBlank
    @Size(min = 13, max = 13, message = "RFC must be exactly 13 characters long")
    @Pattern(regexp = "^[A-Z&Ñ]{3,4}[0-9]{6}[A-Z0-9]{3}$", message = "Invalid RFC format")
    private String rfc;
}
