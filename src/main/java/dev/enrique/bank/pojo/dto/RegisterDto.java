package dev.enrique.bank.pojo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterDto {
    String firstName;
    String lastName;
    String email;
    String password;
    String userRole;
}
