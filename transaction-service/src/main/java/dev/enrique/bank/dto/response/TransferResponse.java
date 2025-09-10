package dev.enrique.bank.dto.response;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class TransferResponse {
    private Long id;
    private BigDecimal amount;
    private String description;
    private String sourceAccountNumber;
    private String targetAccountNumber;
    private TransactionType transactionType;
    private TransactionStatus transactionStatus;
}
