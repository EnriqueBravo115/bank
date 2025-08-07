package dev.enrique.bank.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AuthenticationResponse {
    public AuthUserResponse user;
    private String token;
}
