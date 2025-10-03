package dev.enrique.bank.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.enrique.bank.TransactionServiceTestHelper;
import dev.enrique.bank.commons.dto.response.TransactionSummaryResponse;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.dao.projection.TransactionBasicProjection;
import dev.enrique.bank.dao.projection.TransactionDetailedProjection;
import dev.enrique.bank.service.impl.TransactionAnalyticsServiceImpl;

@ExtendWith(MockitoExtension.class)
public class TransactionAnalyticsServiceTest {
    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionAnalyticsServiceImpl transactionAnalyticsServiceImpl;

    @Test
    void groupTransactionsByType_shouldReturnGroupedTransactions() {
        String accountNumber = "123456";
        TransactionStatus status = TransactionStatus.COMPLETED;

        List<TransactionDetailedProjection> projections = TransactionServiceTestHelper
                .createTransactionDetailedProjection();

        when(transactionRepository.findAllByAccountNumberAndStatus(accountNumber, status,
                TransactionDetailedProjection.class)).thenReturn(projections);

        Map<TransactionType, List<TransactionDetailedProjection>> result = transactionAnalyticsServiceImpl
                .groupTransactionsByType(accountNumber, status);

        assertThat(result).hasSize(2);
        assertThat(result.get(TransactionType.TRANSFER)).hasSize(1);
        assertThat(result.get(TransactionType.SERVICE).get(0).getTransactionCode())
                .isEqualTo(projections.get(0).getTransactionCode());
    }

    @Test
    void sumTransactionByTypeShouldReturn() {
        String accountNumber = "123456";
        TransactionStatus status = TransactionStatus.COMPLETED;

        List<TransactionBasicProjection> projections = TransactionServiceTestHelper
                .createTransactionBasicProjectionList();

        when(transactionRepository.findAllByAccountNumberAndStatus(accountNumber, status,
                TransactionBasicProjection.class)).thenReturn(projections);

        Map<TransactionType, BigDecimal> result = transactionAnalyticsServiceImpl.sumTransactionsByType(accountNumber,
                status);

        assertThat(result).hasSize(2);
        assertEquals(new BigDecimal("400.00"), result.get(TransactionType.TRANSFER));
        assertEquals(new BigDecimal("100.50"), result.get(TransactionType.PURCHASE));
    }

    @Test
    void getTransactionTypeSummary_ShouldReturnTransactionTypeSummary() {
        String accountNumber = "123456";
        TransactionStatus status = TransactionStatus.COMPLETED;

        List<TransactionBasicProjection> projections = TransactionServiceTestHelper
                .createTransactionBasicProjectionList();

        when(transactionRepository.findAllByAccountNumberAndStatus(
                accountNumber, status, TransactionBasicProjection.class))
                .thenReturn(projections);

        Map<TransactionType, TransactionSummaryResponse> result = transactionAnalyticsServiceImpl
                .getTransactionTypeSummary(accountNumber, status);

        assertNotNull(result, "The result must not be null");
        assertEquals(2, result.size(), "There should be 2 types of transactions on the map");

        // TRANSFER
        TransactionSummaryResponse transferSummary = result.get(TransactionType.TRANSFER);
        assertNotNull(transferSummary, "The summary TRANSFER must not be null");
        assertEquals(2, transferSummary.getCount(), "There should be 2 transactions TRANSFER");
        assertEquals(new BigDecimal("400.00"), transferSummary.getTotal(),
                "The total of DEPOSIT must be 400");

        // PURCHASE
        TransactionSummaryResponse purchaseSummary = result.get(TransactionType.PURCHASE);
        assertNotNull(purchaseSummary, "The summary PURCHASE must not be null");
        assertEquals(1, purchaseSummary.getCount(), "There should be 1 transaction PURCHASE");
        assertEquals(new BigDecimal("100.50"), purchaseSummary.getTotal(),
                "The total of PURCHASE must be 100.50");

        verify(transactionRepository).findAllByAccountNumberAndStatus(
                accountNumber, status, TransactionBasicProjection.class);
    }
}
