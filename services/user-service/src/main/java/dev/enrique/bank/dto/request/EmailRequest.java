package dev.enrique.bank.dto.request;

import jakarta.validation.constraints.Email;
import lombok.Data;

import static dev.enrique.bank.commons.constants.ErrorMessage.*;

@Data
public class EmailRequest {
    @Email(regexp = ".+@.+\\..+", message = EMAIL_NOT_VALID)
    private String email;
}
