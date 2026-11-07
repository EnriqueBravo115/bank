package dev.enrique.bank.util;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.IdentifierType;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.model.Transaction;

/** Builds the rows each test class saves for itself (instead of relying on V2__populate_transactions). */
public final class TransactionFixtures {
    public static final String CLABE = "646180110400000001";
    public static final String CARD = "4111111111111111";

    private TransactionFixtures() {
    }

    public static Transaction transaction(String code, String source, TransactionType type,
            TransactionStatus status, String amount, String description, LocalDateTime date) {
        return transaction(JwtTestUtils.OWNER, code, source, type, status, amount, description, date);
    }

    public static Transaction transaction(String ownerId, String code, String source, TransactionType type,
            TransactionStatus status, String amount, String description, LocalDateTime date) {
        return Transaction.builder()
                .ownerId(ownerId)
                .accountNumber(source)
                .transactionCode(code)
                .sourceIdentifier(source)
                .identifierType(CARD.equals(source) ? IdentifierType.CARD : IdentifierType.CLABE)
                .amount(new BigDecimal(amount))
                .description(description)
                .reason(status == TransactionStatus.COMPLETED ? "Valid Transaction" : "Rejected")
                .transactionDate(date)
                .currency(Currency.MX)
                .transactionType(type)
                .transactionStatus(status)
                .build();
    }
}
