package dev.enrique.bank.commons.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResultResponse {
    private BigDecimal amount;
    private TransactionType transactionType;
    private TransactionStatus transactionStatus;
    private String reason;
    private LocalDateTime transactionDate;
}
