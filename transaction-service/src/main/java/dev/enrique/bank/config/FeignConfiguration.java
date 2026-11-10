package dev.enrique.bank.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.web.client.RestClient;

import feign.Logger;
import feign.RequestInterceptor;

@Configuration
public class FeignConfiguration {
    // Same limits as the calls to account-service. Without them, renewing the token while Keycloak is
    // unreachable waits for the OS connect timeout (minutes) and the caller gets a 504 from Kong.
    static final Duration TOKEN_CONNECT_TIMEOUT = Duration.ofSeconds(2);
    static final Duration TOKEN_READ_TIMEOUT = Duration.ofSeconds(5);

    // BASIC: method, URL, status and time. FULL would log the bodies, card number and cvv included.
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    // Caches the token until it expires; works outside of an HTTP request (scheduler).
    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {
        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientService);
        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials(provider -> provider.accessTokenResponseClient(tokenResponseClient()))
                .build());
        return manager;
    }

    @Bean
    public RequestInterceptor serviceTokenInterceptor(ServiceTokenProvider serviceTokenProvider) {
        return template -> template.header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceTokenProvider.token());
    }

    // The default token client of Spring Security, with timeouts (it has none by default).
    private static RestClientClientCredentialsTokenResponseClient tokenResponseClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TOKEN_CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(TOKEN_READ_TIMEOUT);

        RestClientClientCredentialsTokenResponseClient client = new RestClientClientCredentialsTokenResponseClient();
        client.setRestClient(RestClient.builder()
                .requestFactory(requestFactory)
                .messageConverters(converters -> {
                    converters.clear();
                    converters.add(new FormHttpMessageConverter());
                    converters.add(new OAuth2AccessTokenResponseHttpMessageConverter());
                })
                .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler())
                .build());
        return client;
    }
}
