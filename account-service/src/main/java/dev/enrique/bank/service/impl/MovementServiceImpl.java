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

import dev.enrique.bank.commons.dto.request.ClientDepositRequest;
import dev.enrique.bank.commons.dto.request.ClientPurchaseRequest;
import dev.enrique.bank.commons.dto.request.ClientServiceRequest;
import dev.enrique.bank.commons.dto.request.ClientTransferRequest;
import dev.enrique.bank.commons.dto.request.ClientWithdrawalRequest;
import dev.enrique.bank.commons.dto.response.MovementResultResponse;
import dev.enrique.bank.commons.enums.AccountStatus;
import dev.enrique.bank.commons.enums.BalanceType;
import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.LimitType;
import dev.enrique.bank.commons.enums.MovementType;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.exception.AccountBalanceNotFoundException;
import dev.enrique.bank.commons.exception.AccountNotFoundException;
import dev.enrique.bank.commons.exception.AccountNotOperableException;
import dev.enrique.bank.commons.exception.InsufficientFundsException;
import dev.enrique.bank.commons.exception.CurrencyMismatchException;
import dev.enrique.bank.dao.AccountBalanceRepository;
import dev.enrique.bank.dao.AccountRepository;
import dev.enrique.bank.dao.MovementRepository;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.model.AccountBalance;
import dev.enrique.bank.model.Movement;
import dev.enrique.bank.service.CardService;
import dev.enrique.bank.service.FundsValidationService;
import dev.enrique.bank.service.MovementService;
import dev.enrique.bank.service.TransactionLimitService;
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
    private final TransactionLimitService transactionLimitService;
    private final AccountBalanceRepository accountBalanceRepository;
    private final AccountRepository accountRepository;
    private final MovementRepository movementRepository;
    private final CardService cardService;

    @Override
    @Transactional
    public MovementResultResponse processTransfer(String idempotencyKey, ClientTransferRequest request) {
        Movement claim = Movement.builder()
                .movementType(MovementType.TRANSFER)
                .sourceAccountNumber(request.sourceAccountNumber())
                .targetAccountNumber(request.targetAccountNumber())
                .amount(request.amount())
                .build();
        return once(idempotencyKey, claim, accounts -> {
            Account source = accounts.get(request.sourceAccountNumber());
            Account target = accounts.get(request.targetAccountNumber());
            validate(source, request.currency(), request.amount(), request.amount(), LimitType.TRANSFER,
                    idempotencyKey);
            requireOpen(target);
            requireCurrency(target, request.currency());
            adjust(source, request.amount().negate());
            adjust(target, request.amount());
        });
    }

    @Override
    @Transactional
    public MovementResultResponse processPurchase(String idempotencyKey, ClientPurchaseRequest request) {
        Movement claim = debitClaim(MovementType.PURCHASE, request.accountNumber(), request.amount());
        return once(idempotencyKey, claim, accounts -> {
            Account account = accounts.get(request.accountNumber());
            validate(account, request.currency(), request.amount(), request.amount(), LimitType.PURCHASE,
                    idempotencyKey);
            cardService.verifyPayment(account, request.sourceCardNumber(), request.cvv());
            adjust(account, request.amount().negate());
        });
    }

    @Override
    @Transactional
    public MovementResultResponse processService(String idempotencyKey, ClientServiceRequest request) {
        Movement claim = debitClaim(MovementType.SERVICE, request.accountNumber(), request.amount());
        return once(idempotencyKey, claim, accounts -> {
            Account account = accounts.get(request.accountNumber());
            validate(account, request.currency(), request.amount(), request.amount(), LimitType.SERVICE,
                    idempotencyKey);
            adjust(account, request.amount().negate());
        });
    }

    /**
     * Debits the amount plus the fee. Limits count only the amount: the fee is the bank's charge,
     * not money the customer takes out.
     */
    @Override
    @Transactional
    public MovementResultResponse processWithdrawal(String idempotencyKey, ClientWithdrawalRequest request) {
        Movement claim = debitClaim(MovementType.WITHDRAWAL, request.accountNumber(), request.amount());
        claim.setFee(request.transactionFee() == null ? BigDecimal.ZERO : request.transactionFee());
        claim.setVerificationMethod(request.securityVerificationMethod());
        return once(idempotencyKey, claim, accounts -> {
            Account account = accounts.get(request.accountNumber());
            validate(account, request.currency(), request.amount(), claim.totalDebit(), LimitType.WITHDRAWAL,
                    idempotencyKey);
            adjust(account, claim.totalDebit().negate());
        });
    }

    /** Credits money that comes from outside the bank (cash, payroll...): there is no source account. */
    @Override
    @Transactional
    public MovementResultResponse processDeposit(String idempotencyKey, ClientDepositRequest request) {
        Movement claim = Movement.builder()
                .movementType(MovementType.DEPOSIT)
                .targetAccountNumber(request.accountNumber())
                .amount(request.amount())
                .build();
        return once(idempotencyKey, claim, accounts -> {
            Account account = accounts.get(request.accountNumber());
            requireOpen(account);
            requireCurrency(account, request.currency());
            adjust(account, request.amount());
        });
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
        if (movement.getTargetAccountNumber() != null) {
            // The money comes back from the target (transfer or deposit), which must still have it.
            Account target = accounts.get(movement.getTargetAccountNumber());
            requireFunds(target, movement.getAmount());
            adjust(target, movement.getAmount().negate());
        }
        if (movement.getSourceAccountNumber() != null) {
            // The fee of a withdrawal is refunded too.
            adjust(accounts.get(movement.getSourceAccountNumber()), movement.totalDebit());
        }

        movement.setStatus(TransactionStatus.REVERSED);
        movement.setReason(REVERSED);
        log.info("Movement {} ({}) reversed", idempotencyKey, movement.getMovementType());
        return resultOf(movement);
    }

    private static Movement debitClaim(MovementType type, String accountNumber, BigDecimal amount) {
        return Movement.builder().movementType(type).sourceAccountNumber(accountNumber).amount(amount).build();
    }

    /**
     * Applies the movement only the first time its key is seen. The key is claimed before moving the
     * money: a concurrent request with the same key waits on the unique index and fails (409) once
     * this one commits. A rejection rolls the claim back, so a retry is evaluated again.
     * The accounts are locked before reading their balances (see lock).
     */
    private MovementResultResponse once(String idempotencyKey, Movement claim,
            Consumer<Map<String, Account>> apply) {
        Optional<Movement> previous = movementRepository.findByIdempotencyKey(idempotencyKey);
        if (previous.isPresent()) {
            log.info("Movement {} already processed, returning its result", idempotencyKey);
            return resultOf(previous.get());
        }

        claim.setIdempotencyKey(idempotencyKey);
        claim.setStatus(TransactionStatus.COMPLETED);
        claim.setReason(APPLIED);
        movementRepository.saveAndFlush(claim);
        apply.accept(lock(claim.getSourceAccountNumber(), claim.getTargetAccountNumber()));
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

    // debit: what leaves the account (amount + fee); the limits count only the amount.
    private void validate(Account account, Currency currency, BigDecimal amount, BigDecimal debit,
            LimitType limitType, String idempotencyKey) {
        requireOpen(account);
        requireCurrency(account, currency);
        requireFunds(account, debit);
        transactionLimitService.check(account, limitType, amount, idempotencyKey);
    }

    // No currency conversion: a movement must be in the currency of every account it touches.
    private static void requireCurrency(Account account, Currency currency) {
        if (account.getCurrency() != currency) {
            throw new CurrencyMismatchException(account.getAccountNumber(), account.getCurrency(), currency);
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
