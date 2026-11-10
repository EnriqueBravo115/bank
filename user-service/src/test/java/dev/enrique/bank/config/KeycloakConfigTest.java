package dev.enrique.bank.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.ServerSocket;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import dev.enrique.bank.commons.exception.GlobalExceptionHandler;
import jakarta.ws.rs.ProcessingException;

/**
 * Keycloak accepting the connection but never answering: an admin call must fail within the read
 * timeout, and the error is answered as 503 instead of a 500.
 */
class KeycloakConfigTest {

    @Test
    void keycloakThatNeverAnswers_failsWithinTheReadTimeout_andIsA503() throws Exception {
        try (ServerSocket silentKeycloak = new ServerSocket(0)) {
            Keycloak keycloak = adminClient("http://localhost:" + silentKeycloak.getLocalPort());

            long start = System.nanoTime();
            assertThatThrownBy(() -> keycloak.realm("bank").users().count())
                    .isInstanceOfSatisfying(ProcessingException.class, ex ->
                            assertThat(new GlobalExceptionHandler().handleKeycloakUnavailable(ex).getStatusCode())
                                    .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
            Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

            assertThat(elapsed).isGreaterThanOrEqualTo(KeycloakConfig.READ_TIMEOUT.minusMillis(500))
                    .isLessThan(KeycloakConfig.READ_TIMEOUT.plusSeconds(3));
        }
    }

    @Test
    void keycloakRefusingConnections_failsWithProcessingException() throws Exception {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        Keycloak keycloak = adminClient("http://localhost:" + closedPort);

        assertThatThrownBy(() -> keycloak.realm("bank").users().count()).isInstanceOf(ProcessingException.class);
    }

    private static Keycloak adminClient(String serverUrl) {
        KeycloakConfig config = new KeycloakConfig();
        ReflectionTestUtils.setField(config, "authServerUrl", serverUrl);
        ReflectionTestUtils.setField(config, "realm", "bank");
        ReflectionTestUtils.setField(config, "clientId", "user-service");
        ReflectionTestUtils.setField(config, "clientSecret", "secret");
        return config.keycloakAdminClient();
    }
}
