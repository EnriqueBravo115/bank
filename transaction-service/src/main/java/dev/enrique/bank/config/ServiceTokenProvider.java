package dev.enrique.bank.config;

import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * transaction-service's own access token (client credentials of the "transaction-service" client).
 * It works without an incoming request, so the scheduler that retries PENDING transactions can use it.
 */
@Component
@RequiredArgsConstructor
public class ServiceTokenProvider {
    static final String REGISTRATION_ID = "account-service";

    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public String token() {
        OAuth2AuthorizedClient client = authorizedClientManager.authorize(OAuth2AuthorizeRequest
                .withClientRegistrationId(REGISTRATION_ID)
                .principal("transaction-service")
                .build());
        if (client == null) {
            throw new IllegalStateException("Could not obtain a token for account-service");
        }
        return client.getAccessToken().getTokenValue();
    }
}
