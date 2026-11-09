package dev.enrique.bank.commons.util;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

/**
 * 16-digit card numbers (the bank's 6-digit BIN, 9 random digits and the Luhn check digit) and their
 * 3-digit security codes.
 */
@Component
public class CardNumberGenerator {
    // Issuer identification number of the bank's debit cards
    static final String BIN = "412345";

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder number = new StringBuilder(BIN);
        while (number.length() < 15) {
            number.append(random.nextInt(10));
        }
        return number.append(luhnCheckDigit(number)).toString();
    }

    public String generateCvv() {
        return String.format("%03d", random.nextInt(1000));
    }

    static int luhnCheckDigit(CharSequence digits) {
        int sum = 0;
        // Starting from the right, double every other digit; the check digit will be appended to the right
        for (int i = digits.length() - 1, position = 0; i >= 0; i--, position++) {
            int digit = Character.getNumericValue(digits.charAt(i));
            if (position % 2 == 0) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
        }
        return (10 - sum % 10) % 10;
    }
}
