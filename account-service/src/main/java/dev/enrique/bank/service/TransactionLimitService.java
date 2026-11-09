package dev.enrique.bank.service;

import java.math.BigDecimal;
import java.util.List;

import dev.enrique.bank.commons.dto.request.LimitRequest;
import dev.enrique.bank.commons.dto.response.LimitResponse;
import dev.enrique.bank.commons.enums.LimitType;
import dev.enrique.bank.model.Account;

public interface TransactionLimitService {
    /** Creates the limit of the account for that type and period, or replaces it. */
    LimitResponse save(LimitRequest request);

    List<LimitResponse> findByAccountNumber(String accountNumber);

    List<LimitResponse> findByUserId(String userId);

    void delete(Long limitId);

    /**
     * Throws TransactionLimitExceededException if one more movement of the type for that amount
     * goes over any limit of the account. The account must be locked by the caller, so concurrent
     * movements cannot both fit under the same limit. excludedKey: the movement being applied.
     */
    void check(Account account, LimitType limitType, BigDecimal amount, String excludedKey);
}
