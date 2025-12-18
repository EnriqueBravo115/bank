package dev.enrique.bank.service;

import dev.enrique.bank.TransactionServiceTestHelper;
import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.IdentifierType;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.dao.projection.TransactionCommonProjection;
import dev.enrique.bank.dao.projection.TransactionDetailedProjection;
import dev.enrique.bank.model.Transaction;
import dev.enrique.bank.service.impl.TransactionQueryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import static dev.enrique.bank.TransactionServiceTestHelper.*;
import static dev.enrique.bank.commons.constants.TestConstants.CLABE;
import static dev.enrique.bank.commons.constants.TestConstants.STATUS_COMPLETED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@Testcontainers
@ExtendWith(MockitoExtension.class)
public class TransactionQueryServiceTest {
    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionQueryServiceImpl transactionQueryService;

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testDB")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Test
    void getTransactionHistoryShouldReturn() {
        List<TransactionDetailedProjection> projections = generateDetailedProjections();
        when(transactionRepository.findAllBySourceIdentifierAndStatus(CLABE, STATUS_COMPLETED,
                TransactionDetailedProjection.class)).thenReturn(projections);
        List<TransactionDetailedProjection> result = transactionQueryService.getTransactionHistory(CLABE, STATUS_COMPLETED);

        assertEquals(projections, result);
        assertThat(result)
                .extracting(TransactionDetailedProjection::getDescription)
                .contains("Test transaction 3");
    }

    @Test
    void getAllTransactionShouldReturn() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<TransactionCommonProjection> projections = generatePageCommonProjections();
        when(transactionRepository.findAllPageableBySourceIdentifierAndStatus(CLABE, STATUS_COMPLETED, pageable))
                .thenReturn(projections);

        Page<TransactionCommonProjection> result = transactionQueryService
                .getAllTransactions(CLABE, STATUS_COMPLETED, pageable);

        assertEquals(projections, result);
        assertThat(result)
                .extracting(TransactionCommonProjection::getAmount)
                .contains(new BigDecimal("400.00"));
    }

    @Test
    void getTransactionByYearShouldReturn() {
        List<TransactionDetailedProjection> projections = generateDetailedProjections();

        when(transactionRepository.findAllBySourceIdentifierAndStatus(
                CLABE, STATUS_COMPLETED, TransactionDetailedProjection.class)).thenReturn(projections);

        List<TransactionDetailedProjection> result = transactionQueryService.getTransactionsByYear(
                CLABE, STATUS_COMPLETED, 2023);

        verify(transactionRepository, times(1)).findAllBySourceIdentifierAndStatus(
                CLABE, STATUS_COMPLETED, TransactionDetailedProjection.class);
        assertThat(result).hasSize(2);
        assertEquals(projections.get(0).getAmount(), result.get(0).getAmount());
    }

    @Test
    void getAllTransactionsBySourceIdentifiers() {
        List<String> sourceIdentifiers = List.of("123456789123456789", "987654321987654321");

        List<TransactionCommonProjection> projections = generateCommonProjections();
        when(transactionRepository.findAllCompletedBySourceIdentifiersIn(sourceIdentifiers)).thenReturn(projections);

        List<TransactionCommonProjection> result = transactionQueryService.getAllTransactionsBySourceIdentifiers(
                sourceIdentifiers);

        verify(transactionRepository, times(1)).findAllCompletedBySourceIdentifiersIn(sourceIdentifiers);
        assertThat(result).hasSize(3);
        assertEquals(new BigDecimal("300.00"), result.get(0).getAmount());
    }

    @Test
    void getMaxTransaction() {
        List<TransactionCommonProjection> projections = generateCommonProjections();
        when(transactionRepository.findAllCompletedBySourceIdentifier(CLABE, TransactionCommonProjection.class))
                .thenReturn(projections);

        Optional<TransactionCommonProjection> result = transactionQueryService.getMaxTransaction(CLABE, STATUS_COMPLETED);

        assertEquals(projections.get(0), result.orElse(null));
    }

    @Test
    void getTransactionsInDateRange() {
        LocalDateTime startDate = LocalDateTime.of(2025, Month.AUGUST, 10, 10, 10);
        LocalDateTime endDate = LocalDateTime.of(2025, Month.OCTOBER, 10, 10, 10);

        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setSourceIdentifier(CLABE);
        transaction.setTransactionCode("");
        transaction.setAmount(new BigDecimal("400.00"));
        transaction.setDescription("transaction test");
        transaction.setReason("transfer successful");
        transaction.setTransactionDate(LocalDateTime.of(2025, Month.SEPTEMBER, 10, 10, 0));
        transaction.setIdentifierType(IdentifierType.CLABE);
        transaction.setCurrency(Currency.EU);
        transaction.setTransactionType(TransactionType.TRANSFER);
        transaction.setTransactionStatus(STATUS_COMPLETED);

        transactionRepository.save(transaction);
        transactionRepository.flush();

        List<TransactionCommonProjection> result = transactionQueryService.getTransactionsInDateRange(
                CLABE, STATUS_COMPLETED, startDate, endDate);

        Transaction saved = transactionRepository.findById(1L).orElse(null);
        assertThat(saved).isNotNull();
        assertThat(saved.getTransactionDate()).isEqualTo(LocalDateTime.of(2025, Month.SEPTEMBER, 10, 10, 0));

        assertThat(result)
                .hasSize(1)
                .extracting(TransactionCommonProjection::getAmount)
                .containsExactlyInAnyOrder(new BigDecimal("400.00"));
    }
}
