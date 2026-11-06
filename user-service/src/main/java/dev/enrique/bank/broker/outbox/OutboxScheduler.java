package dev.enrique.bank.broker.outbox;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Disabled in tests (outbox.publisher.enabled=false), where there is no Kafka broker.
@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "outbox.publisher.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class OutboxScheduler {
    private final EventOutbox eventOutbox;

    @Value("${outbox.cleanup.sent-retention:P7D}")
    private Duration sentRetention;

    @Scheduled(fixedDelayString = "${outbox.publisher.interval-ms:2000}")
    public void publishPending() {
        eventOutbox.publishPending();
    }

    @Scheduled(cron = "${outbox.cleanup.cron:0 0 3 * * *}")
    public void purgeSent() {
        int deleted = eventOutbox.purgeSent(LocalDateTime.now().minus(sentRetention));
        if (deleted > 0) {
            log.info("Deleted {} outbox events sent more than {} ago", deleted, sentRetention);
        }
    }
}
