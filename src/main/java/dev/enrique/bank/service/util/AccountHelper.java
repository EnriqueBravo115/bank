package dev.enrique.bank.service.util;

import static dev.enrique.bank.commons.constants.ErrorMessage.ACCOUNT_NOT_FOUND;
import java.time.Year;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.commons.exception.ApiRequestException;
import dev.enrique.bank.dao.AccountRepository;
import dev.enrique.bank.model.Account;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountHelper {
    private final AccountRepository accountRepository;

    public void validateAccountId(Long accountId) {
        if (accountId == null)
            throw new ApiRequestException("Account ID cannot be null", HttpStatus.BAD_REQUEST);
        if (accountId <= 0)
            throw new ApiRequestException("Account ID must be positive", HttpStatus.BAD_REQUEST);
        if (!accountRepository.existsById(accountId))
            throw new ApiRequestException("Account not found with id: " + accountId, HttpStatus.NOT_FOUND);
    }

    public void validateAccountIdAndYear(Long accountId, Integer year) {
        validateAccountId(accountId);
        if (year == null)
            throw new ApiRequestException("Year cannot be null", HttpStatus.BAD_REQUEST);

        if (year < 1900 || year > Year.now().getValue() + 1) {
            throw new ApiRequestException("Invalid year. Must be between 1900 and " + (Year.now().getValue() + 1),
                    HttpStatus.BAD_REQUEST);
        }

        if (year > Year.now().getValue())
            throw new ApiRequestException("Cannot query transactions for future years", HttpStatus.BAD_REQUEST);
    }

    public void validateAccountIdAndTransactionType(Long accountId, TransactionType type) {
        validateAccountId(accountId);
        if (type == null)
            throw new ApiRequestException("TransactionType cannot be null", HttpStatus.BAD_REQUEST);
    }

    public Account getAccountById(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ApiRequestException(ACCOUNT_NOT_FOUND, HttpStatus.NOT_FOUND));
    }
}
