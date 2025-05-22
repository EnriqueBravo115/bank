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
public class TransactionDetailedResponse {
    private Long id;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private String description;
    // private String sourceAccountNumber;
    // private String targetAccountNumber;
    private String transactionType;
}
