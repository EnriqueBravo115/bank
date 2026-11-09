package dev.enrique.bank.commons.exception;

import dev.enrique.bank.commons.enums.Currency;

/** A movement in a currency other than the account's (422). */
public class CurrencyMismatchException extends RuntimeException {
    public CurrencyMismatchException(String accountNumber, Currency accountCurrency, Currency requested) {
        super("Account " + accountNumber + " operates in " + accountCurrency + ", not " + requested);
    }
}
