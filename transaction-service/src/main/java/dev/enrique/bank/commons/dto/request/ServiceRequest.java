package dev.enrique.bank.commons.dto.request;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.ServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ServiceRequest(
        @NotNull @Positive BigDecimal amount,
        @NotBlank String description,
        @NotBlank String sourceAccountNumber,
        @NotBlank String paymentReference,
        @NotNull ServiceType serviceType,
        @NotNull Currency currency) {
}
