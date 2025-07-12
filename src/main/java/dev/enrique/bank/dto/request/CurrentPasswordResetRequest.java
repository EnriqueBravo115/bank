package dev.enrique.bank.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static dev.enrique.bank.commons.constants.ErrorMessage.*;

@Data
public class CurrentPasswordResetRequest {

    @NotBlank(message = EMPTY_CURRENT_PASSWORD)
    private String currentPassword;

    @NotBlank(message = EMPTY_PASSWORD)
    @Size(min = 8, message = SHORT_PASSWORD)
    private String password;

    @NotBlank(message = EMPTY_PASSWORD_CONFIRMATION)
    @Size(min = 8, message = SHORT_PASSWORD)
    private String password2;
}
