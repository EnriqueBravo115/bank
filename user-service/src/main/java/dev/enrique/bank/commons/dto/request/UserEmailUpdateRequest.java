package dev.enrique.bank.commons.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserEmailUpdateRequest {
    @NotBlank
    @Email
    private String email;
}
