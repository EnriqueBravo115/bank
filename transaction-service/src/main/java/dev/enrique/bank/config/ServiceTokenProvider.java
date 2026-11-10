package dev.enrique.bank.config;

import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import dev.enrique.bank.commons.exception.AccountServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * transaction-service's own access token (client credentials of the "transaction-service" client).
 * It works without an incoming request, so the scheduler that retries PENDING transactions can use it.
 *
 * Without a token account-service cannot be called: a failure to get one (Keycloak down or slow) is
 * reported as {@link AccountServiceUnavailableException}, so a request gets a 503 and a movement in
 * progress stays PENDING (AccountClient's fallback) to be retried later.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceTokenProvider {
    static final String REGISTRATION_ID = "account-service";

    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public String token() {
        OAuth2AuthorizedClient client;
        try {
            client = authorizedClientManager.authorize(OAuth2AuthorizeRequest
                    .withClientRegistrationId(REGISTRATION_ID)
                    .principal("transaction-service")
                    .build());
        } catch (OAuth2AuthorizationException | RestClientException ex) {
            log.warn("Could not obtain a service token for account-service: {}", ex.toString());
            throw new AccountServiceUnavailableException("account-service is not available, try again later");
        }
        if (client == null) {
            throw new AccountServiceUnavailableException("account-service is not available, try again later");
        }
        return client.getAccessToken().getTokenValue();
    }
}
