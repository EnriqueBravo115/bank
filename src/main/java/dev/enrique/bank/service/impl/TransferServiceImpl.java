package dev.enrique.bank.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.enrique.bank.commons.enums.ScheduledTransferStatus;
import dev.enrique.bank.commons.enums.Status;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.commons.exception.AccountNotFoundException;
import dev.enrique.bank.commons.exception.InsufficientFundsException;
import dev.enrique.bank.commons.exception.ScheduledTransferNotFoundException;
import dev.enrique.bank.commons.exception.TransactionNotFoundException;
import dev.enrique.bank.commons.exception.TransferException;
import dev.enrique.bank.dao.AccountRepository;
import dev.enrique.bank.dao.ScheduledTransferRepository;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.dto.request.TransferRequest;
import dev.enrique.bank.dto.response.TransferResponse;
import dev.enrique.bank.mapper.BasicMapper;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.model.ScheduledTransfer;
import dev.enrique.bank.model.Transaction;
import dev.enrique.bank.service.TransferService;
import dev.enrique.bank.service.util.TransferHelper;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final ScheduledTransferRepository scheduledTransferRepository;
    private final TransferHelper transferHelper;
    private final BasicMapper basicMapper;
    private final ScheduledExecutorService scheduledTransferExecutor;

    // Transfer history grouped in two levels(date -> amount),
    @Override
    public Map<String, Map<String, List<TransferResponse>>> getTransferHistory(Long accountId) {
        return transactionRepository.findAllByAccountId(accountId).stream()
                .collect(transferHelper.twoLevelGroupingBy(
                        t -> t.getTransactionDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        t -> t.getAmount().toString(),
                        t -> basicMapper.convertToResponse(t, TransferResponse.class)));
    }

    // List of transactions by year and sorted
    @Override
    public List<Transaction> getTransactionByYear(Integer year) {
        return transactionRepository.findAll()
                .stream()
                .filter(transaction -> year == null || transaction.getTransactionDate().getYear() == year)
                .sorted()
                .collect(Collectors.toList());
    }

    // podria usar cache
    @Override
    public Page<Transaction> getAllTransfers(Pageable pageable) {
        if (pageable == null)
            throw new IllegalArgumentException("Pageable cannot be null");

        Page<Transaction> transactionsPage = transactionRepository.findAll(pageable);

        if (transactionsPage.isEmpty())
            throw new TransferException("No transactions found");

        return transactionsPage;
    }

    @Override
    @Transactional
    public void transfer(Long sourceAccountId, Long targetAccountId, BigDecimal amount) {
        Account sourceAccount = accountRepository.findById(sourceAccountId)
                .orElseThrow(() -> new TransferException("Source account not found"));

        Account targetAccount = accountRepository.findById(targetAccountId)
                .orElseThrow(() -> new TransferException("Source account not found"));

        if (sourceAccount.getBalance().compareTo(amount) < 0)
            throw new TransferException("Insuficient funds");

        sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
        targetAccount.setBalance(targetAccount.getBalance().add(amount));

        Transaction transaction = Transaction.builder()
                .amount(amount)
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .transactionType(TransactionType.TRANSFER)
                .description("Transfer between accounts")
                .transactionDate(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public void reverseTransfer(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransferException("Transaction not found with id: " + transactionId));

        if (transaction.getTransactionStatus() == TransactionStatus.REVERSED)
            throw new TransferException("Transaction is already reversed");

        if (transaction.getTransactionStatus() != TransactionStatus.COMPLETED)
            throw new TransferException("Only completed can be reversed");

        Account sourceAccount = transaction.getSourceAccount();
        Account targetAccount = transaction.getTargetAccount();

        if (sourceAccount.getStatus() != Status.OPEN || targetAccount.getStatus() != Status.OPEN)
            throw new TransferException("Both accounts must be open to reverse the transaction");

        if (targetAccount.getBalance().compareTo(transaction.getAmount()) < 0)
            throw new TransferException("Insufficient balance in target account to reverse transaction");

        Transaction reversal = Transaction.builder()
                .sourceAccount(targetAccount)
                .targetAccount(sourceAccount)
                .amount(transaction.getAmount())
                .transactionType(TransactionType.REVERSAL)
                .transactionStatus(TransactionStatus.PENDING)
                .originalTransaction(transaction)
                .build();

        sourceAccount.increaseBalance(transaction.getAmount());
        targetAccount.reduceBalance(transaction.getAmount());

        accountRepository.save(sourceAccount);
        accountRepository.save(targetAccount);

        transaction.setTransactionStatus(TransactionStatus.REVERSED);
        transactionRepository.save(transaction);
        transactionRepository.save(reversal);
    }

    @Override
    public boolean hasSufficientFunds(Long accountId, BigDecimal amount) {
        if (accountId == null)
            throw new IllegalArgumentException("Account ID cannot be null");

        if (amount == null)
            throw new IllegalArgumentException("Amount cannot be null");

        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Amount must be greater than zero");

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new TransferException("Account not found with id: " + accountId));

        if (account.getStatus() != Status.OPEN)
            throw new IllegalStateException("Account is not open");

        return account.getBalance().compareTo(amount) >= 0;
    }

    @Override
    public void scheduleTransfer(TransferRequest request, LocalDateTime scheduleDate) {
        Account sourceAccount = accountRepository.findByAccountNumber(request.getSourceAccountNumber())
                .orElseThrow(() -> new TransferException("Source account not found"));

        Account targetAccount = accountRepository.findByAccountNumber(request.getTargetAccountNumber())
                .orElseThrow(() -> new TransferException("Target account not found"));

        transferHelper.validateTransferRequestAndAccounts(request, scheduleDate, sourceAccount, targetAccount,
                request.getAmount());

        ScheduledTransfer scheduledTransfer = ScheduledTransfer.builder()
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .amount(request.getAmount())
                .description(request.getDescription())
                .scheduledDate(scheduleDate)
                .status(ScheduledTransferStatus.PENDING)
                .creationDate(LocalDateTime.now())
                .build();

        scheduledTransferRepository.save(scheduledTransfer);
        scheduleTransferExecution(scheduledTransfer);
    }

    // aun tengo que implementar la logica de cancelacion xd me cago en todo lo
    // cagable tio
    @Override
    public void cancelScheduledTransfer(Long scheduledTransferId) {
        if (scheduledTransferId == null)
            throw new IllegalArgumentException("Scheduled transfer ID cannot be null");

        ScheduledTransfer scheduledTransfer = scheduledTransferRepository.findById(scheduledTransferId)
                .orElseThrow(() -> new ScheduledTransferNotFoundException(
                        "Scheduled transfer not found with id: " + scheduledTransferId));

        if (scheduledTransfer.getStatus() != ScheduledTransferStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING scheduled transfers can be cancelled. Current status: " +
                            scheduledTransfer.getStatus());
        }

        if (scheduledTransfer.getScheduledDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException(
                    "Cannot cancel a scheduled transfer that has already passed its execution date");
        }

        if (scheduledTransfer.getSourceAccount().getStatus() != Status.OPEN ||
                scheduledTransfer.getTargetAccount().getStatus() != Status.OPEN) {
            throw new IllegalStateException("Cannot cancel transfer because one or both accounts are not OPEN");
        }

        scheduledTransfer.setStatus(ScheduledTransferStatus.CANCELLED);
        scheduledTransfer.setCancellationDate(LocalDateTime.now());

        scheduledTransferRepository.save(scheduledTransfer);
    }

    @Override
    public BigDecimal calculateTransferFee(BigDecimal amount, String currency) {
        // Validaciones básicas
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }

        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency cannot be null or empty");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        // Definir las tarifas según la lógica de negocio
        BigDecimal feePercentage;
        BigDecimal minimumFee;
        BigDecimal maximumFee;

        // Lógica de tarifas basada en la moneda
        switch (currency.toUpperCase()) {
            case "USD":
                feePercentage = new BigDecimal("0.02"); // 2%
                minimumFee = new BigDecimal("5.00");
                maximumFee = new BigDecimal("50.00");
                break;

            case "EUR":
                feePercentage = new BigDecimal("0.015"); // 1.5%
                minimumFee = new BigDecimal("4.00");
                maximumFee = new BigDecimal("40.00");
                break;

            case "GBP":
                feePercentage = new BigDecimal("0.025"); // 2.5%
                minimumFee = new BigDecimal("6.00");
                maximumFee = new BigDecimal("60.00");
                break;

            default: // Para otras monedas
                feePercentage = new BigDecimal("0.03"); // 3%
                minimumFee = new BigDecimal("10.00");
                maximumFee = new BigDecimal("100.00");
        }

        // Calcular la tarifa base (porcentaje del monto)
        BigDecimal calculatedFee = amount.multiply(feePercentage);

        // Aplicar mínimo y máximo
        if (calculatedFee.compareTo(minimumFee) < 0) {
            calculatedFee = minimumFee;
        } else if (calculatedFee.compareTo(maximumFee) > 0) {
            calculatedFee = maximumFee;
        }

        // Redondear a 2 decimales (centavos)
        calculatedFee = calculatedFee.setScale(2, RoundingMode.HALF_UP);

        return calculatedFee;
    }

    @Override
    public BigDecimal getTransferLimit(Long accountId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTransferLimit'");
    }

    @Override
    public void notifyTransfer(Long transactionId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'notifyTransfer'");
    }

    @Override
    public void validateTransfer(TransferRequest request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'validateTransfer'");
    }

    private void scheduleTransferExecution(ScheduledTransfer scheduledTransfer) {
        Long delay = ChronoUnit.MILLIS.between(LocalDateTime.now(), scheduledTransfer.getScheduledDate());

        scheduledTransferExecutor.schedule(
                () -> executeScheduledTransfer(scheduledTransfer.getId()),
                delay,
                TimeUnit.MILLISECONDS);
    }

    @Transactional
    public void executeScheduledTransfer(Long transferId) {
        ScheduledTransfer transfer = scheduledTransferRepository.findById(transferId)
                .orElseThrow(() -> new ScheduledTransferNotFoundException(
                        "Scheduled transfer not found with id: " + transferId));

        try {
            if (transfer.getStatus() != ScheduledTransferStatus.PENDING)
                throw new IllegalStateException("Transfer is not in PENDING state");

            // 2. Validar cuentas
            Account source = transfer.getSourceAccount();
            Account target = transfer.getTargetAccount();

            if (source.getStatus() != Status.OPEN || target.getStatus() != Status.OPEN)
                throw new IllegalStateException("One or both accounts are not OPEN");

            // 3. Validar fondos
            if (source.getBalance().compareTo(transfer.getAmount()) < 0)
                throw new InsufficientFundsException("Insufficient funds in source account");

            // 4. Ejecutar transferencia
            source.setBalance(source.getBalance().subtract(transfer.getAmount()));
            target.setBalance(target.getBalance().add(transfer.getAmount()));

            // 5. Actualizar estado
            transfer.setStatus(ScheduledTransferStatus.PROCESSED);
            transfer.setProcessedDate(LocalDateTime.now());

            // 6. Guardar cambios
            accountRepository.save(source);
            accountRepository.save(target);
            scheduledTransferRepository.save(transfer);

            // 7. Crear registro de transacción (opcional)
            Transaction transaction = Transaction.builder()
                    .sourceAccount(source)
                    .targetAccount(target)
                    .amount(transfer.getAmount())
                    .description("Scheduled transfer: " + transfer.getDescription())
                    .transactionDate(LocalDateTime.now())
                    .transactionType(TransactionType.TRANSFER)
                    .transactionStatus(TransactionStatus.COMPLETED)
                    .build();

            transactionRepository.save(transaction);

        } catch (Exception e) {
            transfer.setStatus(ScheduledTransferStatus.FAILED);
            transfer.setErrorMessage(e.getMessage());
            scheduledTransferRepository.save(transfer);

            // Puedes agregar logging aquí
            // logger.error("Failed to execute scheduled transfer ID: " + transferId, e);

            // Relanzar si es necesario
            throw new RuntimeException("Failed to execute scheduled transfer", e);
        }
    }
}
