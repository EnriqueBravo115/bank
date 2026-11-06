package dev.enrique.bank.commons.dto.response;

import java.math.BigDecimal;

public record TransactionSummaryResponse(long count, BigDecimal total) {
}
