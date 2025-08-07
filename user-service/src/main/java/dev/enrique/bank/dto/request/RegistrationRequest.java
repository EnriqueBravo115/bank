package dev.enrique.bank.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import static dev.enrique.bank.constants.ErrorMessage.*;

import dev.enrique.bank.enums.Gender;

@Getter
@Setter
public class RegistrationRequest {
    private String fullName;

    @Email(regexp = ".+@.+\\..+", message = EMAIL_NOT_VALID)
    private String email;

    @NotBlank(message = BLANK_NAME)
    @Size(min = 1, max = 50, message = NAME_NOT_VALID)
    private String username;

    private String birthday;
    private String country;
    private String phoneNumber;
    private String phoneCode;
    private Gender gender;
}
