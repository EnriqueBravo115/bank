package dev.enrique.bank.dto.request;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class EmailRequest {
    @Email(regexp = ".+@.+\\..+", message = "Please enter a valid email address.")
    private String email;
}
