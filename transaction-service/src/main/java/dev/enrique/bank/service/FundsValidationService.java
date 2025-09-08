package dev.enrique.bank.service;

import java.math.BigDecimal;

public interface FundsValidationService {
    Boolean hasSufficientFunds(Long accountId, BigDecimal amount);
    // saldo que el cliente puede usar realmente(bloqueos, retenciones etc)
    BigDecimal getAvailableBalance(Long accountId);
    // hay fondos bloqueados por compras por tarjetas
    Boolean hasSufficientFundsIncludingHolds(Long accountId, BigDecimal amount);
    // evita que un cliente mas de su limite por cuenta
    Boolean isWithinDailyLimit(Long accountId, BigDecimal amount);
    // analizar rangos de limite por tipo de cuenta
    Boolean isWithinTransactionLimit(Long accountId, BigDecimal amount);
}
