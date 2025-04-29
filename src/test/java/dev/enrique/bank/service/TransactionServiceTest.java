package dev.enrique.bank.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import dev.enrique.bank.service.impl.TransferServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.dto.request.TransferRequest;
import dev.enrique.bank.mapper.BasicMapper;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.model.Transaction;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {
    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BasicMapper basicMapper;

    @InjectMocks
    private TransferServiceImpl transferService;

    private Transaction transaction1;
    private Transaction transaction2;
    private TransferRequest transferRequest1;
    private TransferRequest transferRequest2;
    private Account sourceAccount;
    private Account targetAccount;

    @BeforeEach
    void setUp() {
        sourceAccount = Account.builder()
                .id(Long.valueOf(1L))
                .accountNumber("123456789")
                .balance(BigDecimal.valueOf(1000))
                .build();

        targetAccount = Account.builder()
                .id(Long.valueOf(2L))
                .accountNumber("987654321")
                .balance(BigDecimal.valueOf(2000))
                .build();

        LocalDateTime now = LocalDateTime.now();

        transaction1 = Transaction.builder()
                .id(Long.valueOf(1L))
                .amount(BigDecimal.valueOf(500))
                .description("Transferencia 1")
                .transactionDate(now)
                .transactionType(TransactionType.DEPOSIT)
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .build();

        transaction2 = Transaction.builder()
                .id(Long.valueOf(2L))
                .amount(BigDecimal.valueOf(1000))
                .description("Transferencia 2")
                .transactionDate(now.plusSeconds(1))
                .transactionType(TransactionType.DEPOSIT)
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .build();

        transferRequest1 = TransferRequest.builder()
                .id(Long.valueOf(1L))
                .description("Transferencia a familiares")
                .sourceAccountNumber("123456789")
                .targetAccountNumber("987654321")
                .transactionType("TRANSFER")
                .build();

        transferRequest2 = TransferRequest.builder()
                .id(Long.valueOf(2L))
                .description("Transferencia a familiares")
                .sourceAccountNumber("123456789")
                .targetAccountNumber("987654321")
                .transactionType("TRANSFER")
                .build();
    }

    @Test
    void getTransferHistory_ShouldReturnGroupedTransactions() {
        Long accountId = Long.valueOf(1L);
        List<Transaction> transactions = Arrays.asList(transaction1, transaction2);

        when(transactionRepository.findAllByAccountId(accountId)).thenReturn(transactions);
        when(basicMapper.convertToResponse(transaction1, TransferRequest.class)).thenReturn(transferRequest1);
        when(basicMapper.convertToResponse(transaction2, TransferRequest.class)).thenReturn(transferRequest2);

        Map<String, Map<String, List<TransferRequest>>> result = transferService.getTransferHistory(accountId);
        assertNotNull(result);
        assertEquals(2, result.size());
        
        // Verify first transaction
        String dateKey1 = transaction1.getTransactionDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String amountKey1 = transaction1.getAmount().toString();

        assertTrue(result.containsKey(dateKey1));
        assertTrue(result.get(dateKey1).containsKey(amountKey1));
        assertEquals(1, result.get(dateKey1).get(amountKey1).size());
        assertEquals(transferRequest1, result.get(dateKey1).get(amountKey1).get(0));

        // Verify second transaction
        String dateKey2 = transaction2.getTransactionDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String amountKey2 = transaction2.getAmount().toString();

        assertTrue(result.containsKey(dateKey2));
        assertTrue(result.get(dateKey2).containsKey(amountKey2));
        assertEquals(1, result.get(dateKey2).get(amountKey2).size());
        assertEquals(transferRequest2, result.get(dateKey2).get(amountKey2).get(0));

        // Verificar mapeo de campos
        TransferRequest firstRequest = result.get(dateKey1).get(amountKey1).get(0);
        assertEquals("123456789", firstRequest.getSourceAccountNumber());
        assertEquals("987654321", firstRequest.getTargetAccountNumber());
        assertEquals("TRANSFER", firstRequest.getTransactionType());
    }
}
