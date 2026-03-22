package dev.enrique.bank.commons.util;

import java.util.Random;

import org.springframework.stereotype.Component;

@Component
public class ClabeGenerator {
    // Bank code
    private static final String BANK_CODE = "002";
    // City code
    private static final String CITY_CODE = "010";
    private static final int[] WEIGHTS = { 3, 7, 1, 3, 7, 1, 3, 7, 1, 3, 7, 1, 3, 7, 1, 3, 7 };

    public String generate() {
        String accountNumber = generateAccountNumber();
        String clabeWithout = BANK_CODE + CITY_CODE + accountNumber;
        int controlDigit = calculateControlDigit(clabeWithout);
        return clabeWithout + controlDigit;
    }

    private String generateAccountNumber() {
        long timestamp = System.currentTimeMillis() % 100_000_000L;
        int random = new Random().nextInt(999);
        return String.format("%08d%03d", timestamp, random);
    }

    private int calculateControlDigit(String clabe17) {
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (Character.getNumericValue(clabe17.charAt(i)) * WEIGHTS[i]) % 10;
        }
        return (10 - (sum % 10)) % 10;
    }
}
