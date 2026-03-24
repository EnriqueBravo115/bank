package dev.enrique.bank.commons.dto.request;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.SecurityVerificationMethod;

public record ClientWithdrawalRequest(
        String accountNumber,
        BigDecimal amount,
        SecurityVerificationMethod securityVerificationMethod,
        BigDecimal transactionFee) {
}
