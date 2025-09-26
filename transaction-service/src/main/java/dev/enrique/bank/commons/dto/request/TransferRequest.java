package dev.enrique.bank.commons.dto.request;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.Currency;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferRequest extends BaseRequest {
    private final String sourceAccountNumber;
    private final String targetAccountNumber;

    public TransferRequest(BigDecimal amount, String description, Currency currency,
            String sourceAccountNumber, String targetAccountNumber) {
        super(amount, description, currency);
        this.sourceAccountNumber = sourceAccountNumber;
        this.targetAccountNumber = targetAccountNumber;
    }

    public String sourceAccountNumber() {
        return sourceAccountNumber;
    }

    public String targetAccountNumber() {
        return targetAccountNumber;
    }
}
