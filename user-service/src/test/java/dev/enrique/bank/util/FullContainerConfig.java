package dev.enrique.bank.util;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;

import dasniko.testcontainers.keycloak.KeycloakContainer;

public abstract class FullContainerConfig extends PostgresContainerConfig {
    @Container
    static KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.0.6")
            .withRealmImportFile("keycloak/realm-export.json");

    @DynamicPropertySource
    static void configureKeycloak(DynamicPropertyRegistry registry) {
        registry.add("keycloak.auth-server-url", keycloak::getAuthServerUrl);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> keycloak.getAuthServerUrl() + "/realms/bank/protocol/openid-connect/certs");
    }
}
