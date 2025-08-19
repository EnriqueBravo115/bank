package dev.enrique.bank.dto.request;

import static dev.enrique.bank.constants.ErrorMessage.BLANK_NAME;
import static dev.enrique.bank.constants.ErrorMessage.EMAIL_NOT_VALID;
import static dev.enrique.bank.constants.ErrorMessage.NAME_NOT_VALID;

import dev.enrique.bank.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

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
