package dev.enrique.bank.integration;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import dev.enrique.bank.util.PostgresContainerConfig;

/**
 * Prometheus scrapes /actuator/prometheus with a client-credentials token of the "prometheus" client:
 * realm role METRICS and no registration_status. The exposure and the probe groups come from
 * config-server, so they are set here; the security rules are the production ones.
 */
@SpringBootTest(properties = {"management.endpoints.web.exposure.include=health,prometheus",
        "management.endpoint.health.probes.enabled=true"})
@AutoConfigureMockMvc
@AutoConfigureObservability
@ActiveProfiles("test-postgres")
class MetricsEndpointTest extends PostgresContainerConfig {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void prometheusServiceAccount_readsTheMetrics() throws Exception {
        mockMvc.perform(get("/actuator/prometheus").with(jwt().jwt(token -> token.subject("service-account-prometheus"))
                .authorities(new SimpleGrantedAuthority("ROLE_METRICS"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("jvm_memory_used_bytes")));
    }

    @Test
    void customers_cannotReadTheMetrics() throws Exception {
        mockMvc.perform(get("/actuator/prometheus").with(jwt().jwt(token -> token.subject("user-1")
                .claim("registration_status", "COMPLETE"))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER_BASIC"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void withoutToken_metricsAreRejected_butHealthStaysPublic() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    // The Kubernetes probes call them without a token.
    @Test
    void probeGroups_arePublic() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
    }
}
