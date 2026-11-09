package dev.enrique.bank.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.KafkaException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import dev.enrique.bank.broker.Topics;
import dev.enrique.bank.broker.event.UserStatusChangedEvent;
import dev.enrique.bank.broker.outbox.EventOutbox;
import dev.enrique.bank.broker.producer.UserEventProducer;
import dev.enrique.bank.commons.enums.OutboxStatus;
import dev.enrique.bank.dao.OutboxEventRepository;
import dev.enrique.bank.model.OutboxEvent;
import dev.enrique.bank.util.PostgresContainerConfig;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test-postgres")
@AutoConfigureMockMvc
class OutboxControllerTest extends PostgresContainerConfig {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EventOutbox outbox;
    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @MockitoBean
    private UserEventProducer userEventProducer;

    @BeforeEach
    void cleanUp() {
        outboxEventRepository.deleteAll();
    }

    @Test
    void shouldListFailedEvents() throws Exception {
        OutboxEvent failed = failedEvent("kc-1");
        outbox.enqueue(Topics.USER_STATUS_CHANGED, "kc-2", new UserStatusChangedEvent(true));

        mockMvc.perform(get("/api/v1/user/outbox/failed").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(failed.getId().toString()))
                .andExpect(jsonPath("$.content[0].aggregateId").value("kc-1"))
                .andExpect(jsonPath("$.content[0].status").value("FAILED"))
                .andExpect(jsonPath("$.content[0].attempts").value(EventOutbox.MAX_ATTEMPTS))
                .andExpect(jsonPath("$.content[0].lastError").value(org.hamcrest.Matchers.containsString("broker down")))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void shouldRetryFailedEvent() throws Exception {
        OutboxEvent failed = failedEvent("kc-1");

        mockMvc.perform(post("/api/v1/user/outbox/{id}/retry", failed.getId()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.attempts").value(0));

        assertThat(outboxEventRepository.findById(failed.getId()).orElseThrow().getStatus())
                .isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    void shouldRejectRetryOfEventThatIsNotFailed() throws Exception {
        outbox.enqueue(Topics.USER_STATUS_CHANGED, "kc-1", new UserStatusChangedEvent(true));
        UUID id = outboxEventRepository.findAll().get(0).getId();

        mockMvc.perform(post("/api/v1/user/outbox/{id}/retry", id).with(admin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Only FAILED events can be retried, event " + id + " is PENDING"));
    }

    @Test
    void shouldReturnNotFoundForUnknownEvent() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/user/outbox/{id}/retry", id).with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Outbox event not found with id: " + id));
    }

    @Test
    void shouldForbidCustomers() throws Exception {
        OutboxEvent failed = failedEvent("kc-1");

        mockMvc.perform(get("/api/v1/user/outbox/failed").with(customer())).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/user/outbox/{id}/retry", failed.getId()).with(customer()))
                .andExpect(status().isForbidden());

        assertThat(outboxEventRepository.findById(failed.getId()).orElseThrow().getStatus())
                .isEqualTo(OutboxStatus.FAILED);
    }

    private OutboxEvent failedEvent(String keycloakId) {
        when(userEventProducer.send(any(), any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new KafkaException("broker down")));
        outbox.enqueue(Topics.USER_STATUS_CHANGED, keycloakId, new UserStatusChangedEvent(false));
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < EventOutbox.MAX_ATTEMPTS; i++) {
            outbox.publishPending(now);
            now = now.plusHours(1);
        }
        OutboxEvent event = outboxEventRepository.findAll().get(0);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        return event;
    }

    private static RequestPostProcessor admin() {
        return jwt().jwt(j -> j.subject("admin-keycloak-id").claim("registration_status", "COMPLETE"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static RequestPostProcessor customer() {
        return jwt().jwt(j -> j.subject("customer-keycloak-id").claim("registration_status", "COMPLETE"))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER_BASIC"));
    }
}
