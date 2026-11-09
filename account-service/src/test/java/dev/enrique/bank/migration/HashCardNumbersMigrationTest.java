package dev.enrique.bank.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import dev.enrique.bank.commons.util.CardNumberHash;
import dev.enrique.bank.config.migration.V8__hash_card_numbers;
import dev.enrique.bank.util.PostgresContainerConfig;

// Runs the migrations on a schema of its own: up to V7 with a plaintext card number, then V8.
@SpringBootTest
class HashCardNumbersMigrationTest extends PostgresContainerConfig {
    private static final String SCHEMA = "card_migration_test";
    private static final String NUMBER = "4123450000000001";

    @Autowired
    private DataSource dataSource;
    @Autowired
    private V8__hash_card_numbers hashCardNumbers;
    @Autowired
    private CardNumberHash cardNumberHash;

    @Test
    void replacesTheCardNumberWithItsHashAndLastFourDigits() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");

        flyway("7").migrate();
        jdbc.update("INSERT INTO " + SCHEMA + ".account (user_id, email, clabe, account_number, account_type,"
                + " account_status, currency, creation_date) VALUES ('u1', 'u1@test.com', '646180000000000001',"
                + " 'ACC-1', 'CHECKING', 'OPEN', 'MX', NOW())");
        jdbc.update("INSERT INTO " + SCHEMA + ".card (account_id, card_number, card_holder_name, card_type,"
                + " card_status, expiration_date) SELECT id, ?, 'TEST', 'DEBIT', 'ACTIVE', '2030-12-31' FROM "
                + SCHEMA + ".account", NUMBER);

        flyway("8").migrate();

        Map<String, Object> card = jdbc.queryForMap("SELECT * FROM " + SCHEMA + ".card");
        assertThat(card).doesNotContainKey("card_number");
        assertThat(card.get("card_number_hash")).isEqualTo(cardNumberHash.of(NUMBER));
        assertThat(card.get("last_four")).isEqualTo("0001");

        jdbc.execute("DROP SCHEMA " + SCHEMA + " CASCADE");
    }

    private Flyway flyway(String target) {
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas(SCHEMA)
                .locations("classpath:db/migration")
                .javaMigrations(hashCardNumbers)
                .target(target)
                .load();
    }
}
