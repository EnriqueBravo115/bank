package dev.enrique.bank.service;

import java.math.BigDecimal;

public interface FundsValidationService {
    Boolean hasSufficientFunds(String accountNumber, BigDecimal amount);
    // Balance the customer can actually use (after holds, withholdings, etc.)
    BigDecimal getAvailableBalance(String accountNumber);
    // Takes into account funds on hold from card purchases
    Boolean hasSufficientFundsIncludingHolds(String accountNumber, BigDecimal amount);
}
