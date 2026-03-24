package dev.enrique.bank.service;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.LimitType;

public interface FundsValidationService {
    Boolean hasSufficientFunds(String accountNumber, BigDecimal amount);
    // saldo que el cliente puede usar realmente(bloqueos, retenciones etc)
    BigDecimal getAvailableBalance(String accountNumber);
    // hay fondos bloqueados por compras por tarjetas
    Boolean hasSufficientFundsIncludingHolds(String accountNumber, BigDecimal amount);
    // evita que un cliente mas de su limite por cuenta
    Boolean isWithinDailyLimit(String accountNumber, BigDecimal amount, LimitType limitType);
    // analizar rangos de limite por tipo de cuenta
    Boolean isWithinTransactionLimit(String accountNumber, BigDecimal amount, LimitType limitType);
}
