package dev.enrique.bank.commons.dto.request;

import dev.enrique.bank.commons.enums.TransactionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AccountYearRequest {
    @NotBlank(message = "Account number cannot be empty")
    @Positive(message = "Account number must be positive")
    private String accountNumber;

    @NotBlank(message = "Year cannot be empty")
    private Integer year;

    private TransactionStatus status;
}
