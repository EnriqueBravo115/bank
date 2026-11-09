package dev.enrique.bank.service;

import org.springframework.stereotype.Component;

import dev.enrique.bank.client.AccountClient;
import dev.enrique.bank.commons.dto.response.AccountResponse;
import dev.enrique.bank.commons.enums.IdentifierType;
import dev.enrique.bank.commons.exception.AccountNotFoundException;
import dev.enrique.bank.commons.exception.AccountNotOwnedException;
import dev.enrique.bank.commons.exception.AccountServiceUnavailableException;
import dev.enrique.bank.commons.exception.InvalidTransactionException;
import dev.enrique.bank.config.CurrentCaller;
import feign.FeignException;
import lombok.RequiredArgsConstructor;

/**
 * Turns the identifier of a request (account number or CLABE) into the account in account-service,
 * and checks that a customer only moves money out of their own account.
 */
@Component
@RequiredArgsConstructor
public class AccountResolver {
    private final AccountClient accountClient;
    private final CurrentCaller currentCaller;

    public AccountResponse resolve(String identifier, IdentifierType type) {
        try {
            return switch (type) {
                case ACCOUNT -> accountClient.getByAccountNumber(identifier);
                case CLABE -> accountClient.getByClabe(identifier);
                case CARD -> throw new InvalidTransactionException(
                        "Card identifiers are not supported yet: use the account number or the CLABE");
            };
        } catch (FeignException.NotFound ex) {
            throw new AccountNotFoundException("Account not found with " + type + ": " + identifier);
        } catch (FeignException ex) {
            throw new AccountServiceUnavailableException("account-service is not available, try again later");
        }
    }

    /**
     * The account number behind an identifier, to query transactions: they are stored by account
     * number whatever identifier was used to create them. An account number needs no call to
     * account-service; ownership is left to the queries (CurrentCaller.ownerScope).
     */
    public String accountNumberOf(String identifier, IdentifierType type) {
        return type == IdentifierType.ACCOUNT ? identifier : resolve(identifier, type).accountNumber();
    }

    /** The source account of a movement: it must belong to the caller unless they are an admin. */
    public AccountResponse resolveOwned(String identifier, IdentifierType type) {
        AccountResponse account = resolve(identifier, type);
        if (!currentCaller.isAdmin() && !currentCaller.userId().equals(account.userId())) {
            throw new AccountNotOwnedException("Account " + identifier + " does not belong to the user");
        }
        return account;
    }
}
