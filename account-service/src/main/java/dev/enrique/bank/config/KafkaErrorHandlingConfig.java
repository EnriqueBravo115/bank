package dev.enrique.bank.config;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * What happens to an event of user-service that cannot be applied:
 * <ul>
 * <li>A failure of the listener (e.g. the database is down) is retried with exponential backoff
 * (1s, 2s, 4s... up to 30s between attempts, 5 minutes in total by default) before giving up; the
 * partition waits meanwhile, so later events of the same users are not applied ahead of it.</li>
 * <li>After that, or right away for a message that cannot be deserialized (it would fail the same
 * way forever), the record goes to {@code <topic>.DLT} to be inspected and replayed by hand.</li>
 * </ul>
 * The value deserializer is wrapped in ErrorHandlingDeserializer (config-server), so a malformed
 * message reaches this handler instead of failing in poll() over and over.
 */
@Configuration
public class KafkaErrorHandlingConfig {
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaProperties properties,
            @Value("${account.kafka.retry.initial-interval:1s}") Duration initialInterval,
            @Value("${account.kafka.retry.max-interval:30s}") Duration maxInterval,
            @Value("${account.kafka.retry.max-elapsed:5m}") Duration maxElapsed) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(deadLetterTemplates(properties),
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", -1));

        ExponentialBackOff backOff = new ExponentialBackOff(initialInterval.toMillis(), 2.0);
        backOff.setMaxInterval(maxInterval.toMillis());
        backOff.setMaxElapsedTime(maxElapsed.toMillis());

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(DeserializationException.class);
        return handler;
    }

    // Keys are the user ids (strings). A record that could not be deserialized is republished as its
    // original bytes; any other as JSON.
    private static Map<Class<?>, KafkaOperations<?, ?>> deadLetterTemplates(KafkaProperties properties) {
        Map<String, Object> config = properties.buildProducerProperties(null);
        Map<Class<?>, KafkaOperations<?, ?>> templates = new LinkedHashMap<>();
        templates.put(byte[].class, new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(config,
                new StringSerializer(), new ByteArraySerializer())));
        templates.put(Object.class, new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(config,
                new StringSerializer(), new JsonSerializer<>())));
        return templates;
    }
}
