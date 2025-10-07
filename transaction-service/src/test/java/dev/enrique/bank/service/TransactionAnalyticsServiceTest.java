package dev.enrique.bank.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static dev.enrique.bank.commons.constants.TestConstants.*;

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
        List<TransactionDetailedProjection> projections = List.of(
                TransactionServiceTestHelper.createTransactionDetailedProjection("f47ac10b-58cc-4372-a567-0e02b2c3d479",
                        new BigDecimal("100.00"), "Test transaction 1", TransactionType.SERVICE, STATUS_COMPLETED),
                TransactionServiceTestHelper.createTransactionDetailedProjection("a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d",
                        new BigDecimal("200.00"), "Test transaction 2", TransactionType.SERVICE, STATUS_COMPLETED),
                TransactionServiceTestHelper.createTransactionDetailedProjection("9f4e8a2b-1c3d-4e5f-a6b7-8c9d0e1f2a3b",
                        new BigDecimal("100.00"), "Test transaction 3", TransactionType.TRANSFER, STATUS_COMPLETED),
                TransactionServiceTestHelper.createTransactionDetailedProjection("6ba7b810-9dad-11d1-80b4-00c04fd430c8",
                        new BigDecimal("200.00"), "Test transaction 4", TransactionType.TRANSFER, STATUS_COMPLETED));

        when(transactionRepository.findAllBySourceIdentifierAndStatus(CLABE, STATUS_COMPLETED,
                TransactionDetailedProjection.class)).thenReturn(projections);

        Map<TransactionType, List<TransactionDetailedProjection>> result = transactionAnalyticsServiceImpl
                .groupTransactionsByType(CLABE, STATUS_COMPLETED);

        assertThat(result).hasSize(2);
        assertThat(result.get(TransactionType.TRANSFER)).hasSize(2);
        assertThat(result.get(TransactionType.SERVICE).get(0).getTransactionCode()).isEqualTo(projections.get(0).getTransactionCode());
        assertThat(result.get(TransactionType.TRANSFER).get(0).getTransactionCode()).isEqualTo(projections.get(2).getTransactionCode());
    }

    @Test
    void sumTransactionByTypeShouldReturn() {
        List<TransactionBasicProjection> projections = List.of(
                TransactionServiceTestHelper.createTransactionBasicProjection(TransactionType.PURCHASE, new BigDecimal("100.00")),
                TransactionServiceTestHelper.createTransactionBasicProjection(TransactionType.PURCHASE, new BigDecimal("100.00")),
                TransactionServiceTestHelper.createTransactionBasicProjection(TransactionType.TRANSFER, new BigDecimal("200.00")),
                TransactionServiceTestHelper.createTransactionBasicProjection(TransactionType.TRANSFER, new BigDecimal("200.00")));

        when(transactionRepository.findAllBySourceIdentifierAndStatus(CLABE, STATUS_COMPLETED,
                TransactionBasicProjection.class)).thenReturn(projections);

        Map<TransactionType, BigDecimal> result = transactionAnalyticsServiceImpl
                .sumTransactionsByType(CLABE, STATUS_COMPLETED);

        assertThat(result).hasSize(2);
        assertEquals(new BigDecimal("400.00"), result.get(TransactionType.TRANSFER));
        assertEquals(new BigDecimal("200.00"), result.get(TransactionType.PURCHASE));
    }

    @Test
    void partitionTransactionByAmountShouldReturn() {
        List<TransactionBasicProjection> projections = List.of(
                TransactionServiceTestHelper.createTransactionBasicProjection(TransactionType.PURCHASE, new BigDecimal("100.00")),
                TransactionServiceTestHelper.createTransactionBasicProjection(TransactionType.PURCHASE, new BigDecimal("100.00")),
                TransactionServiceTestHelper.createTransactionBasicProjection(TransactionType.TRANSFER, new BigDecimal("200.00")),
                TransactionServiceTestHelper.createTransactionBasicProjection(TransactionType.TRANSFER, new BigDecimal("200.00")));

        when(transactionRepository.findAllBySourceIdentifierAndStatus(CLABE, STATUS_COMPLETED,
                TransactionBasicProjection.class)).thenReturn(projections);

        Map<Boolean, List<TransactionBasicProjection>> result = transactionAnalyticsServiceImpl
                .partitionTransactionsByAmount(CLABE, STATUS_COMPLETED, new BigDecimal("150.00"));

        assertThat(result.get(true)).hasSize(2);
        assertThat(result.get(false)).hasSize(2);
    }

    @Test
    void getTransactionTypeSummary_ShouldReturnTransactionTypeSummary() {
        List<TransactionBasicProjection> projections = List.of(
                TransactionServiceTestHelper.createTransactionBasicProjection(TransactionType.PURCHASE, new BigDecimal("100.00")),
                TransactionServiceTestHelper.createTransactionBasicProjection(TransactionType.PURCHASE, new BigDecimal("100.00")),
                TransactionServiceTestHelper.createTransactionBasicProjection(TransactionType.TRANSFER, new BigDecimal("200.00")),
                TransactionServiceTestHelper.createTransactionBasicProjection(TransactionType.TRANSFER, new BigDecimal("200.00")));

        when(transactionRepository.findAllBySourceIdentifierAndStatus(CLABE, STATUS_COMPLETED,
                TransactionBasicProjection.class)).thenReturn(projections);

        Map<TransactionType, TransactionSummaryResponse> result = transactionAnalyticsServiceImpl
                .getTransactionTypeSummary(CLABE, STATUS_COMPLETED);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(transactionRepository).findAllBySourceIdentifierAndStatus(CLABE, STATUS_COMPLETED, TransactionBasicProjection.class);

        // TRANSFER
        TransactionSummaryResponse transferSummary = result.get(TransactionType.TRANSFER);
        assertNotNull(transferSummary);
        assertEquals(2, transferSummary.getCount());
        assertEquals(new BigDecimal("400.00"), transferSummary.getTotal());

        // PURCHASE
        TransactionSummaryResponse purchaseSummary = result.get(TransactionType.PURCHASE);
        assertNotNull(purchaseSummary);
        assertEquals(2, purchaseSummary.getCount());
        assertEquals(new BigDecimal("200.00"), purchaseSummary.getTotal());
    }

    @Test
    void calculateTotalAmountByStatusAndType() {
        List<TransactionBasicProjection> projections = List.of(
                TransactionServiceTestHelper.createTransactionBasicProjection(TransactionType.TRANSFER,
                        new BigDecimal("200.00")),
                TransactionServiceTestHelper.createTransactionBasicProjection(TransactionType.TRANSFER,
                        new BigDecimal("100.00")));

        when(transactionRepository.findAllBySourceIdentifierStatusAndType(CLABE, STATUS_COMPLETED,
                TransactionType.TRANSFER, TransactionBasicProjection.class)).thenReturn(projections);

        BigDecimal result = transactionAnalyticsServiceImpl
                .calculateTotalAmountByStatusAndType(CLABE, STATUS_COMPLETED, TransactionType.TRANSFER);

        assertEquals(new BigDecimal("300.00"), result);
    }
}
