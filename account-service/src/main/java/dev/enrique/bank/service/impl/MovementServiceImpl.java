package dev.enrique.bank.service.impl;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.enrique.bank.commons.dto.request.ClientPurchaseRequest;
import dev.enrique.bank.commons.dto.request.ClientServiceRequest;
import dev.enrique.bank.commons.dto.request.ClientTransferRequest;
import dev.enrique.bank.commons.dto.request.ClientWithdrawalRequest;
import dev.enrique.bank.commons.dto.response.MovementResultResponse;
import dev.enrique.bank.commons.enums.AccountStatus;
import dev.enrique.bank.commons.enums.BalanceType;
import dev.enrique.bank.commons.enums.LimitType;
import dev.enrique.bank.commons.enums.MovementType;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.exception.AccountBalanceNotFoundException;
import dev.enrique.bank.commons.exception.AccountNotFoundException;
import dev.enrique.bank.commons.exception.AccountNotOperableException;
import dev.enrique.bank.commons.exception.InsufficientFundsException;
import dev.enrique.bank.commons.exception.TransactionLimitExceededException;
import dev.enrique.bank.dao.AccountBalanceRepository;
import dev.enrique.bank.dao.AccountRepository;
import dev.enrique.bank.dao.MovementRepository;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.model.AccountBalance;
import dev.enrique.bank.model.Movement;
import dev.enrique.bank.service.CardService;
import dev.enrique.bank.service.FundsValidationService;
import dev.enrique.bank.service.MovementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovementServiceImpl implements MovementService {
    static final String APPLIED = "Valid Transaction";
    static final String REVERSED = "Reversed";
    static final String CANCELLED = "Cancelled before being applied";

    private final FundsValidationService fundsValidationService;
    private final AccountBalanceRepository accountBalanceRepository;
    private final AccountRepository accountRepository;
    private final MovementRepository movementRepository;
    private final CardService cardService;

    @Override
    @Transactional
    public MovementResultResponse processTransfer(String idempotencyKey, ClientTransferRequest request) {
        return once(idempotencyKey, MovementType.TRANSFER, request.sourceAccountNumber(),
                request.targetAccountNumber(), request.amount(), accounts -> {
                    validate(accounts.get(request.sourceAccountNumber()), request.amount(), LimitType.TRANSFER);
                    requireOpen(accounts.get(request.targetAccountNumber()));
                    adjust(accounts.get(request.sourceAccountNumber()), request.amount().negate());
                    adjust(accounts.get(request.targetAccountNumber()), request.amount());
                });
    }

    @Override
    @Transactional
    public MovementResultResponse processPurchase(String idempotencyKey, ClientPurchaseRequest request) {
        return once(idempotencyKey, MovementType.PURCHASE, request.accountNumber(), null, request.amount(),
                accounts -> {
                    Account account = accounts.get(request.accountNumber());
                    validate(account, request.amount(), LimitType.PURCHASE);
                    cardService.verifyPayment(account, request.sourceCardNumber(), request.cvv());
                    adjust(account, request.amount().negate());
                });
    }

    @Override
    @Transactional
    public MovementResultResponse processService(String idempotencyKey, ClientServiceRequest request) {
        return debit(idempotencyKey, MovementType.SERVICE, request.accountNumber(), request.amount(),
                LimitType.SERVICE);
    }

    @Override
    @Transactional
    public MovementResultResponse processWithdrawal(String idempotencyKey, ClientWithdrawalRequest request) {
        return debit(idempotencyKey, MovementType.WITHDRAWAL, request.accountNumber(), request.amount(),
                LimitType.WITHDRAWAL);
    }

    /**
     * Undoes a COMPLETED movement (REVERSED). If the key was never applied, it is reserved as CANCELLED
     * so a copy of the original request that arrives late is not applied. Repeating it is a no-op.
     */
    @Override
    @Transactional
    public MovementResultResponse reverse(String idempotencyKey) {
        Optional<Movement> found = movementRepository.lockByIdempotencyKey(idempotencyKey);
        if (found.isEmpty()) {
            movementRepository.saveAndFlush(Movement.builder()
                    .idempotencyKey(idempotencyKey)
                    .status(TransactionStatus.CANCELLED)
                    .reason(CANCELLED)
                    .build());
            log.info("Movement {} cancelled before being applied", idempotencyKey);
            return new MovementResultResponse(TransactionStatus.CANCELLED, CANCELLED);
        }

        Movement movement = found.get();
        if (movement.getStatus() != TransactionStatus.COMPLETED) {
            return resultOf(movement);
        }

        Map<String, Account> accounts = lock(movement.getSourceAccountNumber(), movement.getTargetAccountNumber());
        if (movement.getMovementType() == MovementType.TRANSFER) {
            // The money comes back from the target, which must still have it.
            Account target = accounts.get(movement.getTargetAccountNumber());
            requireFunds(target, movement.getAmount());
            adjust(target, movement.getAmount().negate());
        }
        adjust(accounts.get(movement.getSourceAccountNumber()), movement.getAmount());

        movement.setStatus(TransactionStatus.REVERSED);
        movement.setReason(REVERSED);
        log.info("Movement {} ({}) reversed", idempotencyKey, movement.getMovementType());
        return resultOf(movement);
    }

    private MovementResultResponse debit(String idempotencyKey, MovementType type, String accountNumber,
            BigDecimal amount, LimitType limitType) {
        return once(idempotencyKey, type, accountNumber, null, amount, accounts -> {
            Account account = accounts.get(accountNumber);
            validate(account, amount, limitType);
            adjust(account, amount.negate());
        });
    }

    /**
     * Applies the movement only the first time its key is seen. The key is claimed before moving the
     * money: a concurrent request with the same key waits on the unique index and fails (409) once
     * this one commits. A rejection rolls the claim back, so a retry is evaluated again.
     * The accounts are locked before reading their balances (see lock).
     */
    private MovementResultResponse once(String idempotencyKey, MovementType type, String source, String target,
            BigDecimal amount, Consumer<Map<String, Account>> apply) {
        Optional<Movement> previous = movementRepository.findByIdempotencyKey(idempotencyKey);
        if (previous.isPresent()) {
            log.info("Movement {} already processed, returning its result", idempotencyKey);
            return resultOf(previous.get());
        }

        movementRepository.saveAndFlush(Movement.builder()
                .idempotencyKey(idempotencyKey)
                .movementType(type)
                .sourceAccountNumber(source)
                .targetAccountNumber(target)
                .amount(amount)
                .status(TransactionStatus.COMPLETED)
                .reason(APPLIED)
                .build());
        apply.accept(lock(source, target));
        return new MovementResultResponse(TransactionStatus.COMPLETED, APPLIED);
    }

    /**
     * Locks the accounts of a movement (SELECT ... FOR UPDATE) until the transaction ends, so two
     * movements of the same account cannot both read the same balance and spend it twice. They are
     * locked in account number order: two opposite transfers (A to B, B to A) would deadlock otherwise.
     */
    private Map<String, Account> lock(String... accountNumbers) {
        Map<String, Account> locked = new TreeMap<>();
        Stream.of(accountNumbers).filter(Objects::nonNull).distinct().sorted().forEach(number ->
                locked.put(number, accountRepository.lockByAccountNumber(number)
                        .orElseThrow(() -> new AccountNotFoundException(number))));
        return locked;
    }

    private static MovementResultResponse resultOf(Movement movement) {
        return new MovementResultResponse(movement.getStatus(), movement.getReason());
    }

    private void validate(Account account, BigDecimal amount, LimitType limitType) {
        requireOpen(account);
        requireFunds(account, amount);

        String accountNumber = account.getAccountNumber();
        if (!fundsValidationService.isWithinTransactionLimit(accountNumber, amount, limitType)
                || !fundsValidationService.isWithinDailyLimit(accountNumber, amount, limitType)) {
            throw new TransactionLimitExceededException("Amount exceeds the allowed transaction limit");
        }
    }

    private void requireFunds(Account account, BigDecimal amount) {
        if (!fundsValidationService.hasSufficientFunds(account.getAccountNumber(), amount)) {
            throw new InsufficientFundsException(account.getAccountNumber(), amount);
        }
    }

    // A blocked user's account is FROZEN by the user-status-changed event; it must not move money.
    private static void requireOpen(Account account) {
        if (account.getAccountStatus() != AccountStatus.OPEN) {
            throw new AccountNotOperableException(account.getAccountNumber(), account.getAccountStatus());
        }
    }

    // The balance is the latest account_balance row: every change appends a new one.
    private void adjust(Account account, BigDecimal delta) {
        AccountBalance latest = accountBalanceRepository
                .findLatestByAccountNumber(account.getAccountNumber())
                .orElseThrow(() -> new AccountBalanceNotFoundException(account.getAccountNumber()));

        accountBalanceRepository.save(AccountBalance.builder()
                .account(account)
                .balance(latest.getBalance().add(delta))
                .balanceType(BalanceType.INTRADAY)
                .build());
    }
}
