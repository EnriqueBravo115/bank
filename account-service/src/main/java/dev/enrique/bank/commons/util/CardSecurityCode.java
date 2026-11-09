package dev.enrique.bank.commons.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The card security code (CVV) is never stored (PCI-DSS). Like a card issuer does with its card
 * verification key, it is derived from the card number and the expiration date with a secret key:
 * it is shown once when the card is issued and checked later by deriving it again.
 */
@Component
public class CardSecurityCode {
    private static final String ALGORITHM = "HmacSHA256";
    private static final DateTimeFormatter EXPIRATION = DateTimeFormatter.ofPattern("yyMM");

    private final SecretKeySpec key;

    public CardSecurityCode(@Value("${card.cvv-secret}") String secret) {
        if (secret == null || secret.length() < 16) {
            throw new IllegalStateException("card.cvv-secret must have at least 16 characters");
        }
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    public String of(String cardNumber, LocalDate expirationDate) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            byte[] hash = mac.doFinal((cardNumber + EXPIRATION.format(expirationDate))
                    .getBytes(StandardCharsets.UTF_8));
            int value = ByteBuffer.wrap(hash, 0, 4).getInt() & Integer.MAX_VALUE;
            return String.format("%03d", value % 1000);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Cannot derive the card security code", ex);
        }
    }

    /** Constant-time comparison, so the response time does not reveal how many digits match. */
    public boolean matches(String cardNumber, LocalDate expirationDate, String cvv) {
        return cvv != null && MessageDigest.isEqual(
                of(cardNumber, expirationDate).getBytes(StandardCharsets.UTF_8),
                cvv.getBytes(StandardCharsets.UTF_8));
    }
}
