package dev.enrique.bank.commons.enums;

/**
 * Every account_balance row is the balance of the account after an event; the current balance is
 * the latest row. There are no holds: a movement is applied or rejected at once.
 */
public enum BalanceType {
    // 0 when the account is opened
    OPENING,
    // After a movement or its reversal
    MOVEMENT
}
