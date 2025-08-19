package dev.enrique.bank.dto.request;

import static dev.enrique.bank.constants.ErrorMessage.EMAIL_NOT_VALID;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class EmailRequest {
    @Email(regexp = ".+@.+\\..+", message = EMAIL_NOT_VALID)
    private String email;
}
