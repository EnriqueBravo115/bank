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
    // The account the transactions belong to, and the other identifiers it can be used with
    public static final String ACCOUNT = "ACC-1001";
    public static final String CLABE = "646180110400000001";
    public static final String CARD = "4111111111111111";
    // A card is linked to an account of its own in these fixtures
    public static final String CARD_ACCOUNT = "ACC-2002";
    // Account of OTHER_USER that sends transfers to ACCOUNT
    public static final String SENDER_ACCOUNT = "ACC-3003";

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
                .accountNumber(CARD.equals(source) ? CARD_ACCOUNT : ACCOUNT)
                .transactionCode(code)
                .sourceIdentifier(source)
                .identifierType(identifierType(source))
                .amount(new BigDecimal(amount))
                .description(description)
                .reason(status == TransactionStatus.COMPLETED ? "Valid Transaction" : "Rejected")
                .transactionDate(date)
                .currency(Currency.MX)
                .transactionType(type)
                .transactionStatus(status)
                .build();
    }

    /** A transfer from OTHER_USER's SENDER_ACCOUNT to ACCOUNT (owned by OWNER). */
    public static Transaction incomingTransfer(String code, TransactionStatus status, String amount,
            String description, LocalDateTime date) {
        Transaction transfer = transaction(JwtTestUtils.OTHER_USER, code, SENDER_ACCOUNT, TransactionType.TRANSFER,
                status, amount, description, date);
        transfer.setAccountNumber(SENDER_ACCOUNT);
        transfer.setIdentifierType(IdentifierType.ACCOUNT);
        transfer.setCounterpartyAccountNumber(ACCOUNT);
        transfer.setCounterpartyOwnerId(JwtTestUtils.OWNER);
        return transfer;
    }

    private static IdentifierType identifierType(String source) {
        return switch (source) {
            case ACCOUNT -> IdentifierType.ACCOUNT;
            case CARD -> IdentifierType.CARD;
            default -> IdentifierType.CLABE;
        };
    }
}
