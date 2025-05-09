package dev.enrique.bank.commons.enums;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Currency {
    USD(new BigDecimal("0.02"), new BigDecimal("5.00"), new BigDecimal("50.00")),
    EUR(new BigDecimal("0.015"), new BigDecimal("4.00"), new BigDecimal("40.00")),
    GBP(new BigDecimal("0.025"), new BigDecimal("6.00"), new BigDecimal("60.00")),
    MXN(new BigDecimal("0.03"), new BigDecimal("10.00"), new BigDecimal("100.00")),
    OTHER(new BigDecimal("0.03"), new BigDecimal("10.00"), new BigDecimal("100.00"));

    private final BigDecimal feePercentage;
    private final BigDecimal minimumFee;
    private final BigDecimal maximumFee;
}
