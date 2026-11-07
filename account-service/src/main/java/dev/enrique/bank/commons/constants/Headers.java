package dev.enrique.bank.commons.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Headers {
    // transaction-service sends its transaction code: the same key always gets the same result.
    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";
}
