package dev.enrique.bank.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.enrique.bank.commons.dto.request.LimitRequest;
import dev.enrique.bank.commons.dto.response.LimitResponse;
import dev.enrique.bank.commons.enums.LimitType;
import dev.enrique.bank.commons.enums.MovementType;
import dev.enrique.bank.commons.enums.TimePeriod;
import dev.enrique.bank.commons.exception.AccountNotFoundException;
import dev.enrique.bank.commons.exception.LimitNotFoundException;
import dev.enrique.bank.commons.exception.TransactionLimitExceededException;
import dev.enrique.bank.dao.AccountRepository;
import dev.enrique.bank.dao.MovementRepository;
import dev.enrique.bank.dao.MovementUsage;
import dev.enrique.bank.dao.TransactionLimitRepository;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.model.TransactionLimit;
import dev.enrique.bank.service.TransactionLimitService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionLimitServiceImpl implements TransactionLimitService {
    private final TransactionLimitRepository transactionLimitRepository;
    private final AccountRepository accountRepository;
    private final MovementRepository movementRepository;
    private final Clock clock;

    @Override
    @Transactional
    public LimitResponse save(LimitRequest request) {
        Account account = accountRepository.findEntityByAccountNumber(request.accountNumber())
                .orElseThrow(() -> new AccountNotFoundException(request.accountNumber()));
        TransactionLimit limit = transactionLimitRepository
                .findByAccountIdAndLimitTypeAndTimePeriod(account.getId(), request.limitType(), request.timePeriod())
                .orElseGet(() -> TransactionLimit.builder()
                        .account(account)
                        .limitType(request.limitType())
                        .timePeriod(request.timePeriod())
                        .build());
        limit.setMaxAmount(request.maxAmount());
        limit.setMaxTransactions(request.maxTransactions());
        limit.setCurrency(account.getCurrency());
        return LimitResponse.from(transactionLimitRepository.save(limit));
    }

    @Override
    public List<LimitResponse> findByAccountNumber(String accountNumber) {
        return transactionLimitRepository.findAllByAccountNumber(accountNumber).stream()
                .map(LimitResponse::from).toList();
    }

    @Override
    public List<LimitResponse> findByUserId(String userId) {
        return transactionLimitRepository.findAllByUserId(userId).stream().map(LimitResponse::from).toList();
    }

    @Override
    @Transactional
    public void delete(Long limitId) {
        TransactionLimit limit = transactionLimitRepository.findById(limitId)
                .orElseThrow(() -> new LimitNotFoundException(limitId));
        transactionLimitRepository.delete(limit);
    }

    @Override
    public void check(Account account, LimitType limitType, BigDecimal amount, String excludedKey) {
        for (TransactionLimit limit : transactionLimitRepository.findAllByAccountIdAndLimitType(
                account.getId(), limitType)) {
            MovementUsage usage = movementRepository.usageSince(account.getAccountNumber(),
                    MovementType.valueOf(limitType.name()), start(limit.getTimePeriod()), excludedKey);
            String name = limit.getTimePeriod() + " " + limitType + " limit";
            if (usage.count() + 1 > limit.getMaxTransactions()) {
                throw new TransactionLimitExceededException(
                        name + " of " + limit.getMaxTransactions() + " transactions reached");
            }
            if (usage.total().add(amount).compareTo(limit.getMaxAmount()) > 0) {
                throw new TransactionLimitExceededException(name + " of " + money(limit.getMaxAmount()) + " "
                        + limit.getCurrency() + " exceeded: " + money(usage.total()) + " already used");
            }
        }
    }

    // Amounts are stored with 4 decimals; messages show 2
    private static String money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_EVEN).toPlainString();
    }

    // Periods follow the calendar: since midnight, since Monday, since the 1st of the month.
    private LocalDateTime start(TimePeriod period) {
        LocalDate today = LocalDate.now(clock);
        return switch (period) {
            case DAILY -> today.atStartOfDay();
            case WEEKLY -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
            case MONTHLY -> today.withDayOfMonth(1).atStartOfDay();
        };
    }
}
