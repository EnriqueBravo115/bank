package dev.enrique.bank.filter;

import static dev.enrique.bank.constants.ErrorMessage.JWT_TOKEN_EXPIRED;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

import dev.enrique.bank.configuration.JwtProvider;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {
    private final JwtProvider jwtProvider;

    public AuthFilter(JwtProvider jwtProvider) {
        super(Config.class);
        this.jwtProvider = jwtProvider;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String token = jwtProvider.getToken(exchange.getRequest());
            String username = jwtProvider.extractUsername(token);

            if (token != null && jwtProvider.validateToken(token)) {
                log.info("authenticated username: {}", username);
                exchange.getRequest()
                        .mutate()
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
