package dev.enrique.bank.commons.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AccountYearRequest {
    @NotBlank(message = "Account Id cannot be empty")
    @Positive(message = "Account Id must be positive")
    private Long accountId;

    @NotBlank(message = "Year cannot be empty")
    private Integer year;
}
