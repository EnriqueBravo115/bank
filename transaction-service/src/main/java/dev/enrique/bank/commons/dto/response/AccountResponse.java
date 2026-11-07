package dev.enrique.bank.commons.dto.response;

// The fields transaction-service needs from account-service's AccountDetailedResponse.
public record AccountResponse(
        String userId,
        String accountNumber,
        String clabe,
        String accountStatus) {
}
