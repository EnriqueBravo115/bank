package dev.enrique.bank.commons.dto.response;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionBasicResponse {
    private TransactionType transactionType;
    private BigDecimal amount;
}
