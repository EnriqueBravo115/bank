package dev.enrique.bank.broker;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.enrique.bank.broker.producer.UserEventProducer;
import dev.enrique.bank.util.PostgresContainerConfig;

/**
 * What user-service publishes must be what contracts/user-events describes; account-service's
 * UserConsumerTest consumes those same files. Each event is built from its contract example, sent
 * through the real UserEventProducer to an embedded broker and read back as raw JSON.
 */
@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer"
})
@EmbeddedKafka(kraft = true, partitions = 1, topics = {
        Topics.ACCOUNT_REQUESTED, Topics.USER_STATUS_CHANGED, Topics.USER_EMAIL_CHANGED })
@ActiveProfiles("test-postgres")
class UserEventContractTest extends PostgresContainerConfig {
    private static final Path CONTRACTS = Path.of("..", "contracts", "user-events");
    private static final ObjectMapper JSON = new ObjectMapper();
    // A field renamed on this side is an unknown field of the contract: it must fail
    private static final ObjectMapper STRICT = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Autowired
    private UserEventProducer producer;
    @Autowired
    private EmbeddedKafkaBroker broker;

    static Stream<Path> contracts() throws IOException {
        return Files.list(CONTRACTS).filter(path -> path.toString().endsWith(".json")).sorted();
    }

    @Test
    void everyTopicHasAContract() throws IOException {
        List<String> topics = contracts().map(path -> read(path).get("topic").asText()).toList();

        assertThat(topics).containsExactlyInAnyOrder(
                Topics.ACCOUNT_REQUESTED, Topics.USER_STATUS_CHANGED, Topics.USER_EMAIL_CHANGED);
    }

    @ParameterizedTest
    @MethodSource("contracts")
    void publishedEventMatchesTheContract(Path contract) throws Exception {
        JsonNode file = read(contract);
        String topic = file.get("topic").asText();
        String type = file.get("type").asText();
        Object event = STRICT.treeToValue(file.get("payload"), Class.forName(type));
        String userId = "user-" + UUID.randomUUID();

        try (Consumer<String, String> consumer = consumer(topic)) {
            producer.send(topic, userId, event, UUID.randomUUID()).join();

            ConsumerRecord<String, String> record = recordOf(consumer, userId);
            assertThat(record.topic()).isEqualTo(topic);
            assertThat(header(record, "__TypeId__")).isEqualTo(type);
            assertThat(header(record, "auth-user-id")).isEqualTo(userId);
            assertThat(header(record, "event-id")).isNotBlank();
            assertThat(JSON.readTree(record.value())).isEqualTo(file.get("payload"));
        }
    }

    private Consumer<String, String> consumer(String topic) {
        Map<String, Object> props = KafkaTestUtils.consumerProps("contract-" + UUID.randomUUID(), "false", broker);
        Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(),
                new StringDeserializer()).createConsumer();
        broker.consumeFromAnEmbeddedTopic(consumer, topic);
        return consumer;
    }

    // Other tests may publish to the same topic: only this user's record counts
    private static ConsumerRecord<String, String> recordOf(Consumer<String, String> consumer, String userId) {
        long deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline) {
            for (ConsumerRecord<String, String> record : KafkaTestUtils.getRecords(consumer, Duration.ofMillis(500))) {
                if (userId.equals(record.key())) {
                    return record;
                }
            }
        }
        throw new AssertionError("No record published for " + userId);
    }

    private static String header(ConsumerRecord<String, String> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }

    private static JsonNode read(Path path) {
        try {
            return JSON.readTree(path.toFile());
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
