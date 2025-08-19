package dev.enrique.bank.dto.request;

import static dev.enrique.bank.constants.ErrorMessage.EMAIL_NOT_VALID;
import static dev.enrique.bank.constants.ErrorMessage.EMPTY_PASSWORD;
import static dev.enrique.bank.constants.ErrorMessage.SHORT_PASSWORD;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EndRegistrationRequest {
    @Email(regexp = ".+@.+\\..+", message = EMAIL_NOT_VALID)
    private String email;

    @NotBlank(message = EMPTY_PASSWORD)
    @Size(min = 8, message = SHORT_PASSWORD)
    private String password;
}

