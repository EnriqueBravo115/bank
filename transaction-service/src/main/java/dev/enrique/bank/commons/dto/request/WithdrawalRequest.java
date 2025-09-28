package dev.enrique.bank.commons.dto.request;

import java.math.BigDecimal;

import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.SecurityVerificationMethod;
import dev.enrique.bank.commons.enums.WithdrawalMethod;

public record WithdrawalRequest(
        BigDecimal amount,
        String description,
        String atmLocation,
        String atmSessionId,
        String receiptNumber,
        String branchCode,
        String tellerId,
        BigDecimal transactionFee,
        Currency currency,
        SecurityVerificationMethod securityVerificationMethod,
        WithdrawalMethod withdrawalMethod) {
}
