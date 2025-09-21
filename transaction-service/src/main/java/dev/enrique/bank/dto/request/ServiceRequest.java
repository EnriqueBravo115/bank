package dev.enrique.bank.dto.request;

import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.ServiceType;
import dev.enrique.bank.commons.enums.TransactionType;

import java.math.BigDecimal;

public record ServiceRequest(
        BigDecimal amount,
        String description,
        TransactionType transactionType,
        String sourceAccountNumber,
        String paymentReference,
        ServiceType serviceType,
        Currency currency
) {
}
