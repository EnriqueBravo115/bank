package dev.enrique.bank.dto.response;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthUserResponse {
    private Long id;
    private String email;
    private String username;
    private LocalDateTime registrationDate;
    private Boolean isActive;
}
