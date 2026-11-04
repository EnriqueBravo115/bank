package dev.enrique.bank.commons.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Same rules as UserRegisterRequest.
@Getter
@Setter
@NoArgsConstructor
public class UserPhoneUpdateRequest {
    @NotBlank
    @Pattern(regexp = "^(\\+52|\\+1)$")
    private String phoneCode;

    @NotBlank
    @Pattern(regexp = "^\\d{10}$")
    private String phoneNumber;
}
