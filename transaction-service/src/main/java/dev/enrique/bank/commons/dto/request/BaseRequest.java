package dev.enrique.bank.commons.dto.request;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.Currency;
import lombok.Data;

@Data
public abstract class BaseRequest {
    private final BigDecimal amount;
    private final String description;
    private final Currency currency;

    protected BaseRequest(BigDecimal amount, String description, Currency currency) {
        this.amount = amount;
        this.description = description;
        this.currency = currency;
    }
}
