package dev.enrique.bank.filter;

import static dev.enrique.bank.constants.ErrorMessage.EMAIL_NOT_ACTIVATED;
import static dev.enrique.bank.constants.ErrorMessage.JWT_TOKEN_EXPIRED;
import static dev.enrique.bank.constants.FeignConstants.USER_SERVICE;
import static dev.enrique.bank.constants.PathConstants.AUTH_USER_ID_HEADER;
import static dev.enrique.bank.constants.PathConstants.USER_EMAIL;
import static dev.enrique.bank.constants.PathConstants.AUTH;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import dev.enrique.bank.configuration.JwtProvider;
import dev.enrique.bank.dto.UserInfoResponse;
import io.jsonwebtoken.JwtException;

@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {
    private final JwtProvider jwtProvider;
    private final RestTemplate restTemplate;

    public AuthFilter(JwtProvider jwtProvider, RestTemplate restTemplate) {
        super(Config.class);
        this.jwtProvider = jwtProvider;
        this.restTemplate = restTemplate;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String token = jwtProvider.getToken(exchange.getRequest());

            if (token != null && jwtProvider.validateToken(token)) {

                String email = jwtProvider.extractUsername(token);
                UserInfoResponse user = restTemplate.getForObject(
                        String.format("http://%s:8001%s", USER_SERVICE, AUTH + USER_EMAIL),
                        UserInfoResponse.class,
                        email);

                if (user.getActivationCode() != null) {
                    throw new JwtException(EMAIL_NOT_ACTIVATED);
                }

                exchange.getRequest()
                        .mutate()
                        .header(AUTH_USER_ID_HEADER, String.valueOf(user.getId()))
                        .build();
                return chain.filter(exchange);
            } else {
                throw new JwtException(JWT_TOKEN_EXPIRED);
            }
        };
    }

    public static class Config {
    }
}
