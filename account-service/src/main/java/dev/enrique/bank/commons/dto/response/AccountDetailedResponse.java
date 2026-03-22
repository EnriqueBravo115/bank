package dev.enrique.bank.commons.dto.response;

import java.time.LocalDateTime;

import dev.enrique.bank.commons.enums.AccountStatus;
import dev.enrique.bank.commons.enums.AccountType;
import dev.enrique.bank.commons.enums.Currency;

public record AccountDetailedResponse(
        Long id,
        String userId,
        String email,
        String clabe,
        String accountNumber,
        AccountType accountType,
        AccountStatus accountStatus,
        Currency currency,
        LocalDateTime creationDate) {
}
