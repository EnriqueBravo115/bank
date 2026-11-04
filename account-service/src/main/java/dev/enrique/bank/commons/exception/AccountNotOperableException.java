package dev.enrique.bank.commons.exception;

import dev.enrique.bank.commons.enums.AccountStatus;

/** The account exists but is not OPEN (FROZEN, CLOSE, INACTIVE), so it cannot send or receive money. */
public class AccountNotOperableException extends RuntimeException {
    public AccountNotOperableException(String accountNumber, AccountStatus status) {
        super("Account " + accountNumber + " is " + status + " and cannot operate");
    }
}
