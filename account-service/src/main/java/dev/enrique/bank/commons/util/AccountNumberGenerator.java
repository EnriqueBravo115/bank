package dev.enrique.bank.commons.util;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

/**
 * Account numbers: "ACC-" and 16 random digits (20 characters, the size of the column). Unlike the
 * old "ACC-" + millis, two accounts opened in the same millisecond do not collide; the service also
 * checks that the number is free and the column is UNIQUE (V9).
 */
@Component
public class AccountNumberGenerator {
    static final String PREFIX = "ACC-";
    static final int DIGITS = 16;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder number = new StringBuilder(PREFIX);
        // The first digit is never 0, so every number has the same length when read as a number
        number.append(1 + random.nextInt(9));
        while (number.length() < PREFIX.length() + DIGITS) {
            number.append(random.nextInt(10));
        }
        return number.toString();
    }
}
