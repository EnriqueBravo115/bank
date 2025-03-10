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
    private String phoneNumber;
    private String phoneCode;
    private String country;
    private String countryCode;
    private String gender;
    private String birthday;
    private LocalDateTime registrationDate;
    private Boolean isActive;
}
