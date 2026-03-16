package dev.enrique.bank.commons.dto.request;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.SecurityVerificationMethod;

// FIX: needs a source_identifier to identify the account
public record ClientWithdrawalRequest(
        BigDecimal amount,
        SecurityVerificationMethod securityVerificationMethod,
        BigDecimal transactionFee) {
}
