package dev.enrique.bank.event;

import java.math.BigDecimal;

import dev.enrique.bank.model.Account;

public record AccountTrasferEvent(
        BigDecimal amount,
        Account sourceAccount,
        Account targetAccount) {
}
