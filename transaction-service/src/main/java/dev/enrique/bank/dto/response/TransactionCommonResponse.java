package dev.enrique.bank.dto.response;

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
    private String transactionNumber;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
}
