package dev.enrique.bank.commons.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionCommonResponse {
    private String transactionCode;
    private String description;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
}
