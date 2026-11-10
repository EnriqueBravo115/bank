package dev.enrique.bank.config;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.ClientBuilderWrapper;
import org.keycloak.admin.client.JacksonProvider;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

@Configuration
public class KeycloakConfig {
    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    // Without timeouts a call made while Keycloak is unreachable waits for the OS connect timeout
    // (minutes); with them it fails fast and the client gets a 503 (GlobalExceptionHandler).
    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    // Sending the verification email goes through Keycloak's SMTP call: leave room for it.
    static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    @Bean
    public Keycloak keycloakAdminClient() {
        return KeycloakBuilder.builder()
                .serverUrl(authServerUrl)
                .realm(realm)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .resteasyClient(restClient())
                .build();
    }

    // The client the admin library builds by default (ResteasyClientClassicProvider), plus timeouts.
    private static Client restClient() {
        ClientBuilder builder = ClientBuilderWrapper.create(null, false)
                .connectTimeout(CONNECT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        builder.register(JacksonProvider.class, 100);
        return builder.build();
    }
}
