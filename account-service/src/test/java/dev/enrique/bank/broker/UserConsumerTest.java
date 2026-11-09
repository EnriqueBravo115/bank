package dev.enrique.bank.broker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;

import dev.enrique.bank.broker.event.CreateAccountEvent;
import dev.enrique.bank.broker.event.UserEmailChangedEvent;
import dev.enrique.bank.broker.event.UserStatusChangedEvent;
import dev.enrique.bank.commons.enums.AccountStatus;
import dev.enrique.bank.commons.enums.AccountType;
import dev.enrique.bank.commons.enums.BalanceType;
import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.util.ClabeGenerator;
import dev.enrique.bank.dao.AccountRepository;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.model.AccountBalance;
import dev.enrique.bank.util.AccountFixtures;
import dev.enrique.bank.util.PostgresContainerConfig;

/**
 * The events of user-service go through a real (embedded) broker with the same JSON serialization
 * and headers as in production: the event class travels in __TypeId__, the user in auth-user-id.
 */
@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.listener.auto-startup=true",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer",
        "spring.kafka.consumer.properties.spring.json.trusted.packages=*",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer"
})
@EmbeddedKafka(kraft = true, partitions = 1, topics = {
        UserConsumerTest.ACCOUNT_REQUESTED, UserConsumerTest.STATUS_CHANGED, UserConsumerTest.EMAIL_CHANGED })
class UserConsumerTest extends PostgresContainerConfig {
    static final String ACCOUNT_REQUESTED = "user-service.account-requested";
    static final String STATUS_CHANGED = "user-service.user-status-changed";
    static final String EMAIL_CHANGED = "user-service.user-email-changed";
    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures.clear();
    }

    @Test
    void accountRequested_opensTheAccountWithAZeroBalance_once() {
        CreateAccountEvent event = new CreateAccountEvent("new@test.com", AccountType.SAVING, Currency.MX);
        send(ACCOUNT_REQUESTED, "user-new", event);
        // The outbox of user-service delivers at least once: a repeated event is ignored
        send(ACCOUNT_REQUESTED, "user-new", event);

        await().atMost(TIMEOUT).until(() -> accountRepository.findEntityByUserId("user-new").isPresent());
        Account account = accountRepository.findEntityByUserId("user-new").orElseThrow();
        assertThat(account.getEmail()).isEqualTo("new@test.com");
        assertThat(account.getAccountType()).isEqualTo(AccountType.SAVING);
        assertThat(account.getCurrency()).isEqualTo(Currency.MX);
        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.OPEN);
        assertThat(account.getAccountNumber()).matches("ACC-\\d{16}");
        assertThat(ClabeGenerator.isValid(account.getClabe())).isTrue();

        // Both events have been consumed once the status change sent after them is applied
        send(STATUS_CHANGED, "user-new", new UserStatusChangedEvent(false));
        await().atMost(TIMEOUT).until(() -> statusOf("user-new") == AccountStatus.FROZEN);
        assertThat(accountRepository.count()).isOne();
        List<AccountBalance> balances = fixtures.balances();
        assertThat(balances).singleElement().satisfies(balance -> {
            assertThat(balance.getBalance()).isEqualByComparingTo("0");
            assertThat(balance.getBalanceType()).isEqualTo(BalanceType.OPENING);
        });
    }

    @Test
    void userStatusChanged_freezesAndReopensTheAccount() {
        fixtures.account("user-1", "ACC-1", "646180000000000001", AccountStatus.OPEN, "0.00");

        send(STATUS_CHANGED, "user-1", new UserStatusChangedEvent(false));
        await().atMost(TIMEOUT).until(() -> statusOf("user-1") == AccountStatus.FROZEN);

        send(STATUS_CHANGED, "user-1", new UserStatusChangedEvent(true));
        await().atMost(TIMEOUT).until(() -> statusOf("user-1") == AccountStatus.OPEN);
    }

    @Test
    void userStatusChanged_doesNotReopenAnInactiveAccount() {
        fixtures.account("user-1", "ACC-1", "646180000000000001", AccountStatus.INACTIVE, "0.00");
        fixtures.account("user-2", "ACC-2", "646180000000000002", AccountStatus.OPEN, "0.00");

        send(STATUS_CHANGED, "user-1", new UserStatusChangedEvent(true));
        // Same partition: once the later event is applied, the first one was consumed too
        send(STATUS_CHANGED, "user-2", new UserStatusChangedEvent(false));
        await().atMost(TIMEOUT).until(() -> statusOf("user-2") == AccountStatus.FROZEN);

        assertThat(statusOf("user-1")).isEqualTo(AccountStatus.INACTIVE);
    }

    @Test
    void userEmailChanged_updatesTheEmailOfTheAccount() {
        fixtures.account("user-1", "ACC-1", "646180000000000001", AccountStatus.OPEN, "0.00");

        send(EMAIL_CHANGED, "user-1", new UserEmailChangedEvent("changed@test.com"));

        await().atMost(TIMEOUT).until(() -> "changed@test.com".equals(
                accountRepository.findEntityByUserId("user-1").orElseThrow().getEmail()));
    }

    private void send(String topic, String userId, Object event) {
        ProducerRecord<Object, Object> record = new ProducerRecord<>(topic, userId, event);
        record.headers().add("auth-user-id", userId.getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(record).join();
    }

    private AccountStatus statusOf(String userId) {
        return accountRepository.findEntityByUserId(userId).orElseThrow().getAccountStatus();
    }
}
