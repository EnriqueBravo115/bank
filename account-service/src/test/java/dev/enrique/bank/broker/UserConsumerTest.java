package dev.enrique.bank.broker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.enrique.bank.commons.enums.AccountStatus;
import dev.enrique.bank.commons.enums.AccountType;
import dev.enrique.bank.commons.enums.BalanceType;
import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.util.ClabeGenerator;
import dev.enrique.bank.dao.AccountRepository;
import dev.enrique.bank.dao.PendingUserUpdateRepository;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.model.AccountBalance;
import dev.enrique.bank.util.AccountFixtures;
import dev.enrique.bank.util.PostgresContainerConfig;

/**
 * The events go through an embedded broker as user-service sends them: the JSON of
 * contracts/user-events (shared with user-service's UserEventContractTest), the class in the
 * __TypeId__ header and the user in auth-user-id, read with the production deserializer and error handler.
 */
@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.listener.auto-startup=true",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "account.kafka.retry.initial-interval=100ms",
        "account.kafka.retry.max-elapsed=1s"
})
@EmbeddedKafka(kraft = true, partitions = 1, topics = {
        UserConsumerTest.ACCOUNT_REQUESTED, UserConsumerTest.STATUS_CHANGED, UserConsumerTest.EMAIL_CHANGED,
        UserConsumerTest.ACCOUNT_REQUESTED + ".DLT" })
class UserConsumerTest extends PostgresContainerConfig {
    static final String ACCOUNT_REQUESTED = "user-service.account-requested";
    static final String STATUS_CHANGED = "user-service.user-status-changed";
    static final String EMAIL_CHANGED = "user-service.user-email-changed";
    private static final Path CONTRACTS = Path.of("..", "contracts", "user-events");
    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private EmbeddedKafkaBroker broker;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PendingUserUpdateRepository pendingUserUpdateRepository;
    @Autowired
    private AccountFixtures fixtures;

    private KafkaTemplate<String, String> producer;

    @BeforeEach
    void setUp() {
        fixtures.clear();
        pendingUserUpdateRepository.deleteAll();
        Map<String, Object> props = KafkaTestUtils.producerProps(broker);
        producer = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props, new StringSerializer(),
                new StringSerializer()));
    }

    @AfterEach
    void tearDown() {
        producer.destroy();
    }

    @Test
    void accountRequested_opensTheAccountWithAZeroBalance_once() throws IOException {
        send("account-requested", "user-new", Map.of());
        // The outbox of user-service delivers at least once: a repeated event is ignored
        send("account-requested", "user-new", Map.of());

        await().atMost(TIMEOUT).until(() -> accountRepository.findEntityByUserId("user-new").isPresent());
        Account account = accountRepository.findEntityByUserId("user-new").orElseThrow();
        assertThat(account.getEmail()).isEqualTo("ana@test.com");
        assertThat(account.getAccountType()).isEqualTo(AccountType.SAVING);
        assertThat(account.getCurrency()).isEqualTo(Currency.MX);
        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.OPEN);
        assertThat(account.getAccountNumber()).matches("ACC-\\d{16}");
        assertThat(ClabeGenerator.isValid(account.getClabe())).isTrue();

        // Both events have been consumed once the status change sent after them is applied
        send("user-status-changed", "user-new", Map.of("active", false));
        await().atMost(TIMEOUT).until(() -> statusOf("user-new") == AccountStatus.FROZEN);
        assertThat(accountRepository.count()).isOne();
        List<AccountBalance> balances = fixtures.balances();
        assertThat(balances).singleElement().satisfies(balance -> {
            assertThat(balance.getBalance()).isEqualByComparingTo("0");
            assertThat(balance.getBalanceType()).isEqualTo(BalanceType.OPENING);
        });
    }

    @Test
    void userStatusChanged_freezesAndReopensTheAccount() throws IOException {
        fixtures.account("user-1", "ACC-1", "646180000000000001", AccountStatus.OPEN, "0.00");

        send("user-status-changed", "user-1", Map.of("active", false));
        await().atMost(TIMEOUT).until(() -> statusOf("user-1") == AccountStatus.FROZEN);

        send("user-status-changed", "user-1", Map.of("active", true));
        await().atMost(TIMEOUT).until(() -> statusOf("user-1") == AccountStatus.OPEN);
    }

    @Test
    void userStatusChanged_doesNotReopenAnInactiveAccount() throws IOException {
        fixtures.account("user-1", "ACC-1", "646180000000000001", AccountStatus.INACTIVE, "0.00");
        fixtures.account("user-2", "ACC-2", "646180000000000002", AccountStatus.OPEN, "0.00");

        send("user-status-changed", "user-1", Map.of("active", true));
        // Same partition: once the later event is applied, the first one was consumed too
        send("user-status-changed", "user-2", Map.of("active", false));
        await().atMost(TIMEOUT).until(() -> statusOf("user-2") == AccountStatus.FROZEN);

        assertThat(statusOf("user-1")).isEqualTo(AccountStatus.INACTIVE);
    }

    @Test
    void userEmailChanged_updatesTheEmailOfTheAccount() throws IOException {
        fixtures.account("user-1", "ACC-1", "646180000000000001", AccountStatus.OPEN, "0.00");

        send("user-email-changed", "user-1", Map.of());

        await().atMost(TIMEOUT).until(() -> "changed@test.com".equals(
                accountRepository.findEntityByUserId("user-1").orElseThrow().getEmail()));
    }

    @Test
    void changesConsumedBeforeTheAccountIsOpened_areAppliedWhenItIs() throws IOException {
        // Other topics: they can be consumed before the account-requested event sent earlier
        send("user-status-changed", "user-early", Map.of("active", false));
        send("user-email-changed", "user-early", Map.of());
        await().atMost(TIMEOUT).until(() -> pendingUserUpdateRepository.findById("user-early")
                .filter(update -> update.getEmail() != null && Boolean.FALSE.equals(update.getActive()))
                .isPresent());

        send("account-requested", "user-early", Map.of());

        await().atMost(TIMEOUT).until(() -> accountRepository.findEntityByUserId("user-early").isPresent());
        Account account = accountRepository.findEntityByUserId("user-early").orElseThrow();
        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.FROZEN);
        assertThat(account.getEmail()).isEqualTo("changed@test.com");
        assertThat(pendingUserUpdateRepository.findById("user-early")).isEmpty();
    }

    @Test
    void malformedMessage_goesToTheDeadLetterTopic_andLaterEventsAreStillApplied() throws IOException {
        try (Consumer<String, String> dlt = deadLetterConsumer()) {
            producer.send(record(ACCOUNT_REQUESTED, "user-bad", "{not json",
                    "dev.enrique.bank.broker.event.CreateAccountEvent")).join();
            send("account-requested", "user-good", Map.of());

            await().atMost(TIMEOUT).until(() -> accountRepository.findEntityByUserId("user-good").isPresent());
            ConsumerRecord<String, String> dead = deadLetterOf(dlt, "user-bad");
            assertThat(dead.value()).isEqualTo("{not json");
        }
    }

    @Test
    void eventThatKeepsFailing_isRetriedAndThenSentToTheDeadLetterTopic() throws IOException {
        try (Consumer<String, String> dlt = deadLetterConsumer()) {
            // Without an email the account cannot be stored (NOT NULL): it fails on every attempt
            send("account-requested", "user-no-email", java.util.Collections.singletonMap("email", null));

            ConsumerRecord<String, String> dead = deadLetterOf(dlt, "user-no-email");
            assertThat(dead.value()).contains("\"email\":null");
            assertThat(accountRepository.findEntityByUserId("user-no-email")).isEmpty();
        }
    }

    // Sends the contract example of the event, with some fields replaced.
    private void send(String contract, String userId, Map<String, Object> overrides) throws IOException {
        JsonNode file = JSON.readTree(CONTRACTS.resolve(contract + ".json").toFile());
        ObjectNode payload = (ObjectNode) file.get("payload").deepCopy();
        overrides.forEach((field, value) -> payload.set(field, JSON.valueToTree(value)));
        producer.send(record(file.get("topic").asText(), userId, JSON.writeValueAsString(payload),
                file.get("type").asText())).join();
    }

    private static ProducerRecord<String, String> record(String topic, String userId, String value, String type) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, userId, value);
        record.headers().add("__TypeId__", type.getBytes(StandardCharsets.UTF_8));
        record.headers().add("auth-user-id", userId.getBytes(StandardCharsets.UTF_8));
        return record;
    }

    private Consumer<String, String> deadLetterConsumer() {
        Map<String, Object> props = KafkaTestUtils.consumerProps("dlt-reader-" + System.nanoTime(), "false", broker);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(),
                new StringDeserializer()).createConsumer();
        broker.consumeFromAnEmbeddedTopic(consumer, ACCOUNT_REQUESTED + ".DLT");
        return consumer;
    }

    // The dead letter topic is shared by every test: look for the record of this user only.
    private static ConsumerRecord<String, String> deadLetterOf(Consumer<String, String> dlt, String userId) {
        java.util.concurrent.atomic.AtomicReference<ConsumerRecord<String, String>> found =
                new java.util.concurrent.atomic.AtomicReference<>();
        await().atMost(TIMEOUT).until(() -> {
            for (ConsumerRecord<String, String> record : KafkaTestUtils.getRecords(dlt, Duration.ofMillis(500))) {
                if (userId.equals(record.key())) {
                    found.set(record);
                }
            }
            return found.get() != null;
        });
        return found.get();
    }

    private AccountStatus statusOf(String userId) {
        return accountRepository.findEntityByUserId(userId).orElseThrow().getAccountStatus();
    }
}
