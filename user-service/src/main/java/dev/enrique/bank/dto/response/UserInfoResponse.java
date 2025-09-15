package dev.enrique.bank.dto.response;

import lombok.Data;

@Data
public class UserInfoResponse {
    private Long id;
    private String email;
    private String activationCode;
}
