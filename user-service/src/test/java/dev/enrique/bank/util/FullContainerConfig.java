package dev.enrique.bank.util;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import dasniko.testcontainers.keycloak.KeycloakContainer;

public abstract class FullContainerConfig extends PostgresContainerConfig {
    private static final Network NETWORK = Network.newNetwork();

    // Fake SMTP server; the test realm sends its emails to host "mailpit" (see realm-export.json).
    protected static final GenericContainer<?> mailpit = new GenericContainer<>("axllent/mailpit:v1.21")
            .withNetwork(NETWORK)
            .withNetworkAliases("mailpit")
            .withExposedPorts(1025, 8025)
            .waitingFor(Wait.forHttp("/livez").forPort(8025));

    static KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.0.6")
            .withNetwork(NETWORK)
            .withRealmImportFile("keycloak/realm-export.json");

    static {
        mailpit.start();
        keycloak.start();
    }

    @DynamicPropertySource
    static void configureKeycloak(DynamicPropertyRegistry registry) {
        registry.add("keycloak.auth-server-url", keycloak::getAuthServerUrl);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> keycloak.getAuthServerUrl() + "/realms/bank/protocol/openid-connect/certs");
    }

    protected static String mailpitUrl() {
        return "http://" + mailpit.getHost() + ":" + mailpit.getMappedPort(8025);
    }
}
