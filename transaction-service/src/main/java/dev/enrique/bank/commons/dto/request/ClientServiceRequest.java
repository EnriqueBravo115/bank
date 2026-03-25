package dev.enrique.bank.commons.dto.request;

import java.math.BigDecimal;

public record ClientServiceRequest(
        String accountNumber,
        BigDecimal amount) {
}
