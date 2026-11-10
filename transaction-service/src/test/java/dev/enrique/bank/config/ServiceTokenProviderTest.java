package dev.enrique.bank.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.ServerSocket;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import dev.enrique.bank.commons.exception.AccountServiceUnavailableException;

/**
 * Keycloak accepting the connection but never answering (hung, overloaded): getting the service token
 * must fail within the read timeout as "account-service unavailable" (503 / PENDING), not hang the
 * request until Kong gives up.
 */
class ServiceTokenProviderTest {

    @Test
    void tokenEndpointThatNeverAnswers_failsFastAsUnavailable() throws Exception {
        try (ServerSocket silentKeycloak = new ServerSocket(0)) {
            ServiceTokenProvider provider = providerWithTokenUri(
                    "http://localhost:" + silentKeycloak.getLocalPort() + "/token");

            long start = System.nanoTime();
            assertThatThrownBy(provider::token).isInstanceOf(AccountServiceUnavailableException.class);
            Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

            assertThat(elapsed).isGreaterThanOrEqualTo(FeignConfiguration.TOKEN_READ_TIMEOUT.minusMillis(500))
                    .isLessThan(FeignConfiguration.TOKEN_READ_TIMEOUT.plusSeconds(3));
        }
    }

    @Test
    void tokenEndpointRefusingConnections_failsAsUnavailable() throws Exception {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        ServiceTokenProvider provider = providerWithTokenUri("http://localhost:" + closedPort + "/token");

        assertThatThrownBy(provider::token).isInstanceOf(AccountServiceUnavailableException.class);
    }

    private static ServiceTokenProvider providerWithTokenUri(String tokenUri) {
        ClientRegistration registration = ClientRegistration.withRegistrationId(ServiceTokenProvider.REGISTRATION_ID)
                .clientId("transaction-service")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri(tokenUri)
                .build();
        InMemoryClientRegistrationRepository registrations = new InMemoryClientRegistrationRepository(registration);
        return new ServiceTokenProvider(new FeignConfiguration().authorizedClientManager(
                registrations, new InMemoryOAuth2AuthorizedClientService(registrations)));
    }
}
