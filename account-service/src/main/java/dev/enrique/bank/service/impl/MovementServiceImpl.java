package dev.enrique.bank.service.impl;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.stereotype.Service;

import dev.enrique.bank.commons.dto.request.ClientPurchaseRequest;
import dev.enrique.bank.commons.dto.request.ClientServiceRequest;
import dev.enrique.bank.commons.dto.request.ClientTransferRequest;
import dev.enrique.bank.commons.dto.request.ClientWithdrawalRequest;
import dev.enrique.bank.commons.dto.response.MovementResultResponse;
import dev.enrique.bank.commons.enums.BalanceType;
import dev.enrique.bank.commons.enums.LimitType;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.exception.AccountNotFoundException;
import dev.enrique.bank.dao.AccountBalanceRepository;
import dev.enrique.bank.model.AccountBalance;
import dev.enrique.bank.service.FundsValidationService;
import dev.enrique.bank.service.MovementService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovementServiceImpl implements MovementService {
    private final FundsValidationService fundsValidationService;
    private final AccountBalanceRepository accountBalanceRepository;

    @Override
    @Transactional
    public MovementResultResponse processTransfer(ClientTransferRequest request) {
        Optional<MovementResultResponse> validation = validate(
                request.sourceAccountNumber(), request.amount(), LimitType.TRANSFER, false);
        if (validation.isPresent())
            return validation.get();

        debit(request.sourceAccountNumber(), request.amount());

        return new MovementResultResponse(TransactionStatus.COMPLETED, "Valid Transaction");
    }

    @Override
    @Transactional
    public MovementResultResponse processPurchase(ClientPurchaseRequest request) {
        Optional<MovementResultResponse> validation = validate(
                request.accountNumber(), request.amount(), LimitType.PURCHASE, true);

        if (validation.isPresent())
            return validation.get();

        debit(request.accountNumber(), request.amount());
        return new MovementResultResponse(TransactionStatus.COMPLETED, "Valid Transaction");
    }

    @Override
    @Transactional
    public MovementResultResponse processService(ClientServiceRequest request) {
        Optional<MovementResultResponse> validation = validate(
                request.accountNumber(), request.amount(), LimitType.SERVICE, false);

        if (validation.isPresent())
            return validation.get();

        debit(request.accountNumber(), request.amount());
        return new MovementResultResponse(TransactionStatus.COMPLETED, "Valid Transaction");
    }

    @Override
    @Transactional
    public MovementResultResponse processWithdrawal(ClientWithdrawalRequest request) {
        Optional<MovementResultResponse> validation = validate(
                request.accountNumber(), request.amount(), LimitType.WITHDRAWAL, true);

        if (validation.isPresent())
            return validation.get();

        debit(request.accountNumber(), request.amount());
        return new MovementResultResponse(TransactionStatus.COMPLETED, "Valid Transaction");
    }

    private Optional<MovementResultResponse> validate(String accountNumber, BigDecimal amount,
            LimitType limitType, boolean checkHolds) {
        if (!fundsValidationService.hasSufficientFunds(accountNumber, amount)) {
            return Optional.of(new MovementResultResponse(
                    TransactionStatus.DECLINED, "Insufficient funds"));
        }

        if (checkHolds && !fundsValidationService.hasSufficientFundsIncludingHolds(accountNumber, amount)) {
            return Optional.of(new MovementResultResponse(
                    TransactionStatus.DECLINED, "Insufficient funds considering active holds"));
        }

        if (!fundsValidationService.isWithinTransactionLimit(accountNumber, amount, limitType)) {
            return Optional.of(new MovementResultResponse(
                    TransactionStatus.DECLINED, "Amount exceeds the allowed transaction limit"));
        }

        if (!fundsValidationService.isWithinDailyLimit(accountNumber, amount, limitType)) {
            return Optional.of(new MovementResultResponse(
                    TransactionStatus.DECLINED, "Amount exceeds the allowed daily limit"));
        }

        return Optional.empty();
    }

    private void debit(String accountNumber, BigDecimal amount) {
        AccountBalance latest = accountBalanceRepository
                .findLatestByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        accountBalanceRepository.save(AccountBalance.builder()
                .account(latest.getAccount())
                .balance(latest.getBalance().subtract(amount))
                .balanceType(BalanceType.INTRADAY)
                .build());
    }
}
