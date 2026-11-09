package dev.enrique.bank.service.impl;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.enrique.bank.client.AccountClient;
import dev.enrique.bank.commons.dto.request.DepositRequest;
import dev.enrique.bank.commons.dto.request.PurchaseRequest;
import dev.enrique.bank.commons.dto.request.ServiceRequest;
import dev.enrique.bank.commons.dto.request.TransferRequest;
import dev.enrique.bank.commons.dto.request.WithdrawalRequest;
import dev.enrique.bank.commons.dto.response.AccountResponse;
import dev.enrique.bank.commons.dto.response.MovementResultResponse;
import dev.enrique.bank.commons.dto.response.TransactionResultResponse;
import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.IdentifierType;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.commons.exception.AccountServiceUnavailableException;
import dev.enrique.bank.commons.exception.InvalidTransactionException;
import dev.enrique.bank.commons.exception.TransactionNotFoundException;
import dev.enrique.bank.commons.exception.TransactionNotReversibleException;
import dev.enrique.bank.config.CurrentCaller;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.model.PurchaseTransaction;
import dev.enrique.bank.model.ServiceTransaction;
import dev.enrique.bank.model.Transaction;
import dev.enrique.bank.model.TransferTransaction;
import dev.enrique.bank.model.WithdrawalTransaction;
import dev.enrique.bank.service.AccountResolver;
import dev.enrique.bank.service.TransactionCreationService;
import dev.enrique.bank.settlement.TransactionSettlement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionCreationServiceImpl implements TransactionCreationService {
    private final AccountResolver accountResolver;
    private final TransactionSettlement settlement;
    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;
    private final CurrentCaller currentCaller;

    @Override
    public TransactionResultResponse transfer(TransferRequest request) {
        AccountResponse source = accountResolver.resolveOwned(request.sourceIdentifier(), request.sourceIdentifierType());
        AccountResponse target = accountResolver.resolve(request.targetIdentifier(), request.targetIdentifierType());
        if (source.accountNumber().equals(target.accountNumber())) {
            throw new InvalidTransactionException("Source and target accounts must be different");
        }

        Transaction transaction = transaction(TransactionType.TRANSFER, source, request.sourceIdentifier(),
                request.sourceIdentifierType(), request.amount(), request.description(), request.currency());
        transaction.setCounterpartyAccountNumber(target.accountNumber());
        transaction.setCounterpartyOwnerId(target.userId());
        TransferTransaction transfer = TransferTransaction.builder()
                .targetIdentifier(request.targetIdentifier())
                .targetIdentifierType(request.targetIdentifierType())
                .targetAccountNumber(target.accountNumber())
                .transaction(transaction)
                .build();
        transaction.setTransferTransaction(transfer);
        return register(transaction);
    }

    @Override
    public TransactionResultResponse purchase(PurchaseRequest request) {
        AccountResponse source = accountResolver.resolveOwned(request.sourceAccountNumber(), IdentifierType.ACCOUNT);
        Transaction transaction = transaction(TransactionType.PURCHASE, source, request.sourceAccountNumber(),
                IdentifierType.ACCOUNT, request.amount(), request.description(), request.currency());
        transaction.setPurchaseTransaction(PurchaseTransaction.builder()
                .sourceCardNumber(request.sourceCardNumber())
                .cvv(request.cvv())
                .merchantCode(request.merchantCode())
                .merchantCategory(request.merchantCategory())
                .posId(request.posId())
                .transaction(transaction)
                .build());
        return register(transaction);
    }

    @Override
    public TransactionResultResponse servicePayment(ServiceRequest request) {
        AccountResponse source = accountResolver.resolveOwned(request.sourceAccountNumber(), IdentifierType.ACCOUNT);
        Transaction transaction = transaction(TransactionType.SERVICE, source, request.sourceAccountNumber(),
                IdentifierType.ACCOUNT, request.amount(), request.description(), request.currency());
        transaction.setServiceTransaction(ServiceTransaction.builder()
                .paymentReference(request.paymentReference())
                .serviceType(request.serviceType())
                .transaction(transaction)
                .build());
        return register(transaction);
    }

    @Override
    public TransactionResultResponse withdrawal(WithdrawalRequest request) {
        AccountResponse source = accountResolver.resolveOwned(request.sourceAccountNumber(), IdentifierType.ACCOUNT);
        Transaction transaction = transaction(TransactionType.WITHDRAWAL, source, request.sourceAccountNumber(),
                IdentifierType.ACCOUNT, request.amount(), request.description(), request.currency());
        transaction.setWithdrawalTransaction(WithdrawalTransaction.builder()
                .atmLocation(request.atmLocation())
                .atmSessionId(request.atmSessionId())
                .receiptNumber(request.receiptNumber())
                .branchCode(request.branchCode())
                .tellerId(request.tellerId())
                .transactionFee(request.transactionFee())
                .securityVerificationMethod(request.securityVerificationMethod())
                .withdrawalMethod(request.withdrawalMethod())
                .transaction(transaction)
                .build());
        return register(transaction);
    }

    /**
     * Credits an account with money from outside the bank. Admin only (see the controller), so the
     * account need not belong to the caller: the transaction belongs to the account's owner, who sees
     * it in their history.
     */
    @Override
    public TransactionResultResponse deposit(DepositRequest request) {
        AccountResponse target = accountResolver.resolve(request.targetIdentifier(), request.targetIdentifierType());
        return register(transaction(TransactionType.DEPOSIT, target, request.targetIdentifier(),
                request.targetIdentifierType(), request.amount(), request.description(), request.currency()));
    }

    /**
     * A PENDING transaction whose result is still unknown: account-service undoes it if it was
     * applied (REVERSED) or blocks its key so it is never applied (CANCELLED). Owner or admin.
     */
    @Override
    @Transactional
    public TransactionResultResponse cancel(String transactionCode) {
        Transaction transaction = lockVisible(transactionCode);
        if (transaction.getTransactionStatus() != TransactionStatus.PENDING) {
            throw new TransactionNotReversibleException("Only PENDING transactions can be cancelled, this one is "
                    + transaction.getTransactionStatus());
        }
        return undo(transaction);
    }

    // A COMPLETED transaction is returned to the source account. Admin only (see the controller).
    @Override
    @Transactional
    public TransactionResultResponse reverse(String transactionCode) {
        Transaction transaction = lockVisible(transactionCode);
        if (transaction.getTransactionStatus() != TransactionStatus.COMPLETED) {
            throw new TransactionNotReversibleException("Only COMPLETED transactions can be reversed, this one is "
                    + transaction.getTransactionStatus());
        }
        return undo(transaction);
    }

    private TransactionResultResponse undo(Transaction transaction) {
        MovementResultResponse result = accountClient.reverse(transaction.getTransactionCode());
        switch (result.transactionStatus()) {
            case REVERSED, CANCELLED -> transaction.registerAttempt(result.transactionStatus(), result.reason(), null);
            case PENDING -> throw new AccountServiceUnavailableException(
                    "account-service is not available, try again later");
            default -> throw new TransactionNotReversibleException(
                    "account-service could not undo the transaction: " + result.reason());
        }
        log.info("Transaction {} {} by {}", transaction.getTransactionCode(), result.transactionStatus(),
                currentCaller.userId());
        return TransactionResultResponse.from(transaction);
    }

    // Locks the row so the PENDING retry scheduler (SKIP LOCKED) leaves it alone meanwhile.
    private Transaction lockVisible(String transactionCode) {
        return transactionRepository.lockByTransactionCode(transactionCode)
                .filter(t -> currentCaller.isAdmin() || currentCaller.userId().equals(t.getOwnerId()))
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Transaction not found with code: " + transactionCode));
    }

    private TransactionResultResponse register(Transaction transaction) {
        return TransactionResultResponse.from(settlement.register(transaction));
    }

    private static Transaction transaction(TransactionType type, AccountResponse source, String sourceIdentifier,
            IdentifierType identifierType, BigDecimal amount, String description, Currency currency) {
        return Transaction.builder()
                .transactionType(type)
                .sourceIdentifier(sourceIdentifier)
                .identifierType(identifierType)
                .accountNumber(source.accountNumber())
                .ownerId(source.userId())
                .amount(amount)
                .description(description)
                .currency(currency)
                .build();
    }
}
