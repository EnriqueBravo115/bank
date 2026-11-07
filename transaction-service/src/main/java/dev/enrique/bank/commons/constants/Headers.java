package dev.enrique.bank.commons.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Headers {
    // The transaction code: account-service applies each key only once.
    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";
}
