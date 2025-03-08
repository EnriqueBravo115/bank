package dev.enrique.bank.model.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthenticationResponse {
    private AuthUserResponse user;
    private String token;
}
