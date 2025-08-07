package dev.enrique.bank.dto.request;

import static dev.enrique.bank.constants.ErrorMessage.EMPTY_CURRENT_PASSWORD;
import static dev.enrique.bank.constants.ErrorMessage.EMPTY_PASSWORD;
import static dev.enrique.bank.constants.ErrorMessage.EMPTY_PASSWORD_CONFIRMATION;
import static dev.enrique.bank.constants.ErrorMessage.SHORT_PASSWORD;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
