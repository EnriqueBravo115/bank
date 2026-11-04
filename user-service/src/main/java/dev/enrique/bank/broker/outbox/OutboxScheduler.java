package dev.enrique.bank.broker.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.RequiredArgsConstructor;

// Disabled in tests (outbox.publisher.enabled=false), where there is no Kafka broker.
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "outbox.publisher.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class OutboxScheduler {
    private final EventOutbox eventOutbox;

    @Scheduled(fixedDelayString = "${outbox.publisher.interval-ms:2000}")
    public void publishPending() {
        eventOutbox.publishPending();
    }
}
