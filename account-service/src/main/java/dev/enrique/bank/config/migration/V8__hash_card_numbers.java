package dev.enrique.bank.config.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.stereotype.Component;

import dev.enrique.bank.commons.util.CardNumberHash;

/**
 * Replaces the plaintext card number with its keyed hash and its last four digits. It is a Java
 * migration because the hash needs card.pan-secret: Spring Boot hands the JavaMigration beans to
 * Flyway, which runs them in version order along with the SQL ones.
 */
@Component
public class V8__hash_card_numbers extends BaseJavaMigration {
    private final CardNumberHash cardNumberHash;

    public V8__hash_card_numbers(CardNumberHash cardNumberHash) {
        this.cardNumberHash = cardNumberHash;
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE card ADD COLUMN card_number_hash VARCHAR(64), ADD COLUMN last_four VARCHAR(4)");
        }
        try (Statement select = connection.createStatement();
                ResultSet cards = select.executeQuery("SELECT id, card_number FROM card");
                PreparedStatement update = connection.prepareStatement(
                        "UPDATE card SET card_number_hash = ?, last_four = ? WHERE id = ?")) {
            while (cards.next()) {
                String number = cards.getString("card_number");
                update.setString(1, cardNumberHash.of(number));
                update.setString(2, CardNumberHash.lastFour(number));
                update.setLong(3, cards.getLong("id"));
                update.addBatch();
            }
            update.executeBatch();
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    ALTER TABLE card
                        ALTER COLUMN card_number_hash SET NOT NULL,
                        ALTER COLUMN last_four SET NOT NULL,
                        ADD CONSTRAINT uk_card_number_hash UNIQUE (card_number_hash),
                        DROP COLUMN card_number
                    """);
        }
    }
}
