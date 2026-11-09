package dev.enrique.bank.commons.dto.response;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.LimitType;
import dev.enrique.bank.commons.enums.TimePeriod;
import dev.enrique.bank.model.TransactionLimit;

public record LimitResponse(
        Long id,
        String accountNumber,
        LimitType limitType,
        TimePeriod timePeriod,
        BigDecimal maxAmount,
        Integer maxTransactions,
        Currency currency) {

    public static LimitResponse from(TransactionLimit limit) {
        return new LimitResponse(limit.getId(), limit.getAccount().getAccountNumber(), limit.getLimitType(),
                limit.getTimePeriod(), limit.getMaxAmount(), limit.getMaxTransactions(), limit.getCurrency());
    }
}
