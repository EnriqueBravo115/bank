package dev.enrique.bank.service;

import java.math.BigDecimal;

public interface FundsValidationService {
    Boolean hasSufficientFunds(String accountNumber, BigDecimal amount);
}
