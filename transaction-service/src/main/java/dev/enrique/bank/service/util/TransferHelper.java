package dev.enrique.bank.service.util;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import dev.enrique.bank.commons.enums.AccountStatus;
import dev.enrique.bank.commons.exception.ApiRequestException;
import dev.enrique.bank.dto.response.AccountResponse;
import dev.enrique.bank.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransferHelper {
    public void validateTransfer(
            AccountResponse sourceAccount,
            AccountResponse targetAccount,
            BigDecimal amount) {

        if (sourceAccount.getBalance().compareTo(amount) < 0) {
            throw new ApiRequestException("Insufficient funds in source account", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (sourceAccount.getAccountStatus() != AccountStatus.OPEN
                || targetAccount.getAccountStatus() != AccountStatus.OPEN) {
            throw new ApiRequestException("Both accounts must be open", HttpStatus.CONFLICT);
        }
    }

    public void validateReverseTransfer(Transaction transaction, Account sourceAccount, Account targetAccount) {
        if (transaction.getTransactionStatus() == TransactionStatus.REVERSED)
            throw new IllegalStateException("Transaction is already reversed");

        if (transaction.getTransactionStatus() != TransactionStatus.COMPLETED)
            throw new IllegalStateException("Only completed can be reversed");

        if (sourceAccount.getStatus() != Status.OPEN || targetAccount.getStatus() != Status.OPEN)
            throw new IllegalStateException("Both accounts must be open to reverse the transaction");

        if (targetAccount.getBalance().compareTo(transaction.getAmount()) < 0)
            throw new IllegalStateException("Insufficient balance in target account to reverse transaction");
    }
}
