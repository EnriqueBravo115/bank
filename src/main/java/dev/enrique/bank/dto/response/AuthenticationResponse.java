package dev.enrique.bank.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthenticationResponse {
    private AuthUserResponse user;
    private String token;
}
