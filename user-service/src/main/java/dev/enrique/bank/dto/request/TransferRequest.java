package dev.enrique.bank.dto.request;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class TransferRequest {
    private Long id;
    private BigDecimal amount;
    private String description;
    private String sourceAccountNumber;
    private String targetAccountNumber;
    private TransactionType transactionType;
}
