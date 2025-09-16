package dev.enrique.bank.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountResponse {
    BigDecimal balance;
}
