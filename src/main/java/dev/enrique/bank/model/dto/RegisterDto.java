package dev.enrique.bank.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterDto {
    String username;
    String email;
    String password;
    String phoneNumber;
    String country;
    String gender;
}
