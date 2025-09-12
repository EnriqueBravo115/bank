package dev.enrique.bank.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TransactionSummaryResponse {
    private int count;
    private BigDecimal total;
}
