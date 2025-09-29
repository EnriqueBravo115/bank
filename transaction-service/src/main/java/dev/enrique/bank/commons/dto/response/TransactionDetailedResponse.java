package dev.enrique.bank.commons.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDetailedResponse {
    private String transactionCode;
    private BigDecimal amount;
    private String description;
    private LocalDateTime transactionDate;
    private TransactionType transactionType;
    private TransactionStatus transactionStatus;
}
