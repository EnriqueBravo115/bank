package dev.enrique.bank.dto.request;

import static dev.enrique.bank.constants.ErrorMessage.CURP_SIZE;
import static dev.enrique.bank.constants.ErrorMessage.EMAIL_NOT_VALID;
import static dev.enrique.bank.constants.ErrorMessage.INVALID_CURP_FORMAT;
import static dev.enrique.bank.constants.ErrorMessage.INVALID_DATE_FORMAT;
import static dev.enrique.bank.constants.ErrorMessage.INVALID_PHONE_CODE;
import static dev.enrique.bank.constants.ErrorMessage.INVALID_PHONE_FORMAT;
import static dev.enrique.bank.constants.ErrorMessage.INVALID_RFC_FORMAT;
import static dev.enrique.bank.constants.ErrorMessage.LETTERS_AND_SPACES_ONLY;
import static dev.enrique.bank.constants.ErrorMessage.RFC_SIZE;

import dev.enrique.bank.enums.Country;
import dev.enrique.bank.enums.Gender;
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
    @Email(message = EMAIL_NOT_VALID)
    private String email;

    @Pattern(regexp = "^(\\+52|\\+1)$", message = INVALID_PHONE_CODE)
    private String phoneCode;

    @Pattern(regexp = "^\\d{10}$", message = INVALID_PHONE_FORMAT)
    private String phoneNumber;

    @NotBlank
    @Pattern(regexp = "^[\\p{L} ']+$", message = LETTERS_AND_SPACES_ONLY)
    private String names;

    @NotBlank
    @Pattern(regexp = "^[\\p{L} ']+$", message = LETTERS_AND_SPACES_ONLY)
    private String firstSurname;

    @NotBlank
    @Pattern(regexp = "^[\\p{L} ']+$", message = LETTERS_AND_SPACES_ONLY)
    private String secondSurname;

    @NotBlank
    private Gender gender;

    @NotBlank
    @Pattern(regexp = "^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/(19[2-9][0-9]|20[0-9]{2})$",
             message = INVALID_DATE_FORMAT)
    private String birthday;

    @NotBlank
    private Country countryOfBirth;

    @NotBlank
    @Size(min = 18, max = 18, message = CURP_SIZE)
    @Pattern(regexp = "^[A-Z]{4}[0-9]{6}[HM][A-Z]{5}[0-9A-Z]{2}$", message = INVALID_CURP_FORMAT)
    private String curp;

    @NotBlank
    @Size(min = 13, max = 13, message = RFC_SIZE)
    @Pattern(regexp = "^[A-Z&Ñ]{3,4}[0-9]{6}[A-Z0-9]{3}$", message = INVALID_RFC_FORMAT)
    private String rfc;
}
