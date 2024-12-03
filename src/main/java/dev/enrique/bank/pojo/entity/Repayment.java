package dev.enrique.bank.pojo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Repayment {
    private LocalDateTime paymentDate;
    private BigDecimal amount;
}
