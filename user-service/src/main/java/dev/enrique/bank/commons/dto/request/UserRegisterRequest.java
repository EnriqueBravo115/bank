package dev.enrique.bank.commons.dto.request;

import java.util.Locale;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserRegisterRequest {
    @NotBlank
    @Email
    private String email;

    // Stored lowercase: emails are compared and kept unique ignoring case.
    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    @NotBlank
    private String password;

    @NotBlank
    @Pattern(regexp = "^(\\+52|\\+1)$")
    private String phoneCode;

    @NotBlank
    @Pattern(regexp = "^\\d{10}$")
    private String phoneNumber;
}
