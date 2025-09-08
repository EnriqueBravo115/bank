package dev.enrique.bank.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.transaction.TransactionStatus;

import dev.enrique.bank.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDetailedResponse {
    private String transactionNumber;
    private BigDecimal amount;
    private String description;
    private LocalDateTime transactionDate;
    private TransactionType transactionType;
    private TransactionStatus transactionStatus;
}
