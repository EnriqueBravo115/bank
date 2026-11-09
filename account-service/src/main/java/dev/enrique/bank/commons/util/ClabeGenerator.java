package dev.enrique.bank.commons.util;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

/**
 * 18-digit CLABE: bank code (3), city code (3), account number (11) and the check digit computed
 * as Banxico specifies (weights 3, 7, 1 modulo 10).
 */
@Component
public class ClabeGenerator {
    static final String BANK_CODE = "002";
    static final String CITY_CODE = "010";
    private static final int[] WEIGHTS = { 3, 7, 1 };

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder clabe = new StringBuilder(BANK_CODE).append(CITY_CODE);
        while (clabe.length() < 17) {
            clabe.append(random.nextInt(10));
        }
        return clabe.append(checkDigit(clabe)).toString();
    }

    public static boolean isValid(String clabe) {
        return clabe != null && clabe.matches("\\d{18}")
                && checkDigit(clabe.substring(0, 17)) == Character.getNumericValue(clabe.charAt(17));
    }

    static int checkDigit(CharSequence first17) {
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += Character.getNumericValue(first17.charAt(i)) * WEIGHTS[i % 3] % 10;
        }
        return (10 - sum % 10) % 10;
    }
}
