package dev.enrique.bank.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BearerToken {
    private String accessToken;
    private String tokenType;

    public BearerToken(String accessToken, String tokenType) {
        this.tokenType = tokenType;
        this.accessToken = accessToken;
    }
}
