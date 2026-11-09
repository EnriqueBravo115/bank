package dev.enrique.bank.service;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.LimitType;

public interface FundsValidationService {
    Boolean hasSufficientFunds(String accountNumber, BigDecimal amount);
    // Balance the customer can actually use (after holds, withholdings, etc.)
    BigDecimal getAvailableBalance(String accountNumber);
    // Takes into account funds on hold from card purchases
    Boolean hasSufficientFundsIncludingHolds(String accountNumber, BigDecimal amount);
    // Keeps a customer from spending more than the account's daily limit
    Boolean isWithinDailyLimit(String accountNumber, BigDecimal amount, LimitType limitType);
    // Checks the per-transaction limit of the account for the movement type
    Boolean isWithinTransactionLimit(String accountNumber, BigDecimal amount, LimitType limitType);
}
