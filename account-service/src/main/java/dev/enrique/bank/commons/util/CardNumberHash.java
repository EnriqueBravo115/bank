package dev.enrique.bank.commons.util;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The card number (PAN) is not stored (PCI-DSS): only a keyed hash, to find the card by its number,
 * and the last four digits, to show it. A plain SHA-256 would not do: the 10^9 numbers of a BIN can
 * be hashed in seconds, so the hash needs a secret key (card.pan-secret).
 */
@Component
public class CardNumberHash {
    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;

    public CardNumberHash(@Value("${card.pan-secret}") String secret) {
        if (secret == null || secret.length() < 16) {
            throw new IllegalStateException("card.pan-secret must have at least 16 characters");
        }
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    public String of(String cardNumber) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal(cardNumber.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Cannot hash the card number", ex);
        }
    }

    public static String lastFour(String cardNumber) {
        return cardNumber.substring(cardNumber.length() - 4);
    }
}
