package dev.enrique.bank.service;

import dev.enrique.bank.TransactionServiceTestHelper;
import dev.enrique.bank.commons.dto.response.TransactionSummaryResponse;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.dao.projection.TransactionBasicProjection;
import dev.enrique.bank.dao.projection.TransactionCommonProjection;
import dev.enrique.bank.dao.projection.TransactionDetailedProjection;
import dev.enrique.bank.service.impl.TransactionAnalyticsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Month;
import java.util.List;
import java.util.Map;

import static dev.enrique.bank.commons.constants.TestConstants.CLABE;
import static dev.enrique.bank.commons.constants.TestConstants.STATUS_COMPLETED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionAnalyticsServiceTest {
    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionAnalyticsServiceImpl transactionAnalyticsServiceImpl;

    @Test
    void groupTransactionsByType_shouldGroupProjectionsCorrectlyByTransactionType() {
        List<TransactionDetailedProjection> projections = TransactionServiceTestHelper.generateDetailedProjections();

        when(transactionRepository.findAllBySourceIdentifierAndStatus(CLABE, STATUS_COMPLETED,
                TransactionDetailedProjection.class)).thenReturn(projections);

        Map<TransactionType, List<TransactionDetailedProjection>> result = transactionAnalyticsServiceImpl
                .groupTransactionsByType(CLABE, STATUS_COMPLETED);

        assertThat(result).containsKeys(TransactionType.TRANSFER, TransactionType.SERVICE);
        assertThat(result.get(TransactionType.TRANSFER)).hasSize(2);
        assertThat(result.get(TransactionType.SERVICE))
                .extracting(TransactionDetailedProjection::getDescription)
                .containsExactly("Test transaction 1", "Test transaction 2");
        verify(transactionRepository).findAllBySourceIdentifierAndStatus(CLABE, STATUS_COMPLETED, TransactionDetailedProjection.class);
    }

    @Test
    void sumTransactionsByType_shouldReturnCorrectTotalsForEachTransactionType() {
        List<TransactionBasicProjection> projections = TransactionServiceTestHelper.generateBasicProjections();

        when(transactionRepository.findAllBySourceIdentifierAndStatus(CLABE, STATUS_COMPLETED,
                TransactionBasicProjection.class)).thenReturn(projections);

        Map<TransactionType, BigDecimal> result = transactionAnalyticsServiceImpl
                .sumTransactionsByType(CLABE, STATUS_COMPLETED);

        assertThat(result)
                .containsEntry(TransactionType.TRANSFER, new BigDecimal("300.00"))
                .containsEntry(TransactionType.PURCHASE, new BigDecimal("300.00"));
        verify(transactionRepository).findAllBySourceIdentifierAndStatus(CLABE, STATUS_COMPLETED, TransactionBasicProjection.class);
    }

    @Test
    void partitionTransactionsByAmount_shouldPartitionCorrectlyAboveAndBelowThreshold() {
        List<TransactionBasicProjection> projections = TransactionServiceTestHelper.generateBasicProjections();

        when(transactionRepository.findAllBySourceIdentifierAndStatus(CLABE, STATUS_COMPLETED,
                TransactionBasicProjection.class)).thenReturn(projections);

        Map<Boolean, List<TransactionBasicProjection>> result = transactionAnalyticsServiceImpl
                .partitionTransactionsByAmount(CLABE, STATUS_COMPLETED, new BigDecimal("150.00"));

        assertThat(result.get(true)).hasSize(2);
        assertThat(result.get(false)).hasSize(2);
        assertThat(result.get(true)).allMatch(tx -> tx.getAmount().compareTo(new BigDecimal("150.00")) > 0);
        assertThat(result.get(false)).allMatch(tx -> tx.getAmount().compareTo(new BigDecimal("150.00")) <= 0);
    }

    @Test
    void getTransactionTypeSummary_shouldReturnSummaryWithCountAndTotalForEachType() {
        List<TransactionBasicProjection> projections = TransactionServiceTestHelper.generateBasicProjections();

        when(transactionRepository.findAllBySourceIdentifierAndStatus(CLABE, STATUS_COMPLETED,
                TransactionBasicProjection.class)).thenReturn(projections);

        Map<TransactionType, TransactionSummaryResponse> result = transactionAnalyticsServiceImpl
                .getTransactionTypeSummary(CLABE, STATUS_COMPLETED);

        assertEquals(2, result.size());
        verify(transactionRepository).findAllBySourceIdentifierAndStatus(CLABE, STATUS_COMPLETED, TransactionBasicProjection.class);

        // TRANSFER
        TransactionSummaryResponse transferSummary = result.get(TransactionType.TRANSFER);
        assertNotNull(transferSummary);
        assertEquals(2, transferSummary.getCount());
        assertEquals(new BigDecimal("300.00"), transferSummary.getTotal());

        // PURCHASE
        TransactionSummaryResponse purchaseSummary = result.get(TransactionType.PURCHASE);
        assertNotNull(purchaseSummary);
        assertEquals(2, purchaseSummary.getCount());
        assertEquals(new BigDecimal("300.00"), purchaseSummary.getTotal());
    }

    @Test
    void calculateTotalAmountByStatusAndType_shouldReturnSumOfAllMatchingTransactions() {
        List<TransactionBasicProjection> projections = List.of(
                TransactionServiceTestHelper.createTransactionBasicProjection(TransactionType.TRANSFER, new BigDecimal("200.00")),
                TransactionServiceTestHelper.createTransactionBasicProjection(TransactionType.TRANSFER, new BigDecimal("100.00")));

        when(transactionRepository.findAllBySourceIdentifierStatusAndType(CLABE, STATUS_COMPLETED,
                TransactionType.TRANSFER, TransactionBasicProjection.class)).thenReturn(projections);

        BigDecimal result = transactionAnalyticsServiceImpl
                .calculateTotalAmountByStatusAndType(CLABE, STATUS_COMPLETED, TransactionType.TRANSFER);

        assertEquals(new BigDecimal("300.00"), result);
        verify(transactionRepository).findAllBySourceIdentifierStatusAndType(CLABE, STATUS_COMPLETED,
                TransactionType.TRANSFER, TransactionBasicProjection.class);
    }

    @Test
    void getAverageDaysBetweenTransactions_shouldReturnAverageDaysDifferenceBetweenConsecutiveTransactions() {
        List<TransactionCommonProjection> projections = TransactionServiceTestHelper.generateCommonProjections();

        when(transactionRepository.findAllBySourceIdentifierAndStatus(CLABE, STATUS_COMPLETED,
                TransactionCommonProjection.class)).thenReturn(projections);

        Double result = transactionAnalyticsServiceImpl.getAverageDaysBetweenTransactions(CLABE, STATUS_COMPLETED);

        assertEquals(2, result);
        verify(transactionRepository).findAllBySourceIdentifierAndStatus(CLABE, STATUS_COMPLETED, TransactionCommonProjection.class);
    }

    @Test
    void getMaxTransactionByType_shouldReturnTransactionWithHighestAmountPerType() {
        List<TransactionBasicProjection> projections = TransactionServiceTestHelper.generateBasicProjections();

        when(transactionRepository.findAllBySourceIdentifierAndStatus(CLABE, STATUS_COMPLETED, TransactionBasicProjection.class))
                .thenReturn(projections);

        Map<TransactionType, List<TransactionBasicProjection>> result = transactionAnalyticsServiceImpl
                .getMaxTransactionByType(CLABE, STATUS_COMPLETED);

        assertThat(result.get(TransactionType.TRANSFER).get(0).getAmount())
                .isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    void countTransactionsByMonth_shouldReturnCorrectTransactionCountGroupedByMonth() {
        List<TransactionCommonProjection> projections = TransactionServiceTestHelper.generateCommonProjections();

        when(transactionRepository.findAllBySourceIdentifierAndStatus(CLABE, STATUS_COMPLETED,
                TransactionCommonProjection.class)).thenReturn(projections);

        Map<Month, Long> result = transactionAnalyticsServiceImpl.countTransactionsByMonth(CLABE, STATUS_COMPLETED);

        assertThat(result).containsEntry(Month.OCTOBER, 3L);
    }

    @Test
    void getAverageAmountByType_shouldReturnCorrectAverageForEachTransactionType() {
        List<TransactionBasicProjection> projections = TransactionServiceTestHelper.generateBasicProjections();

        when(transactionRepository.findAllBySourceIdentifierAndStatus(CLABE, STATUS_COMPLETED, TransactionBasicProjection.class))
                .thenReturn(projections);

        Map<TransactionType, BigDecimal> result = transactionAnalyticsServiceImpl.getAverageAmountByType(CLABE, STATUS_COMPLETED);

        assertThat(result.get(TransactionType.TRANSFER)).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(result.get(TransactionType.PURCHASE)).isEqualByComparingTo(new BigDecimal("150.00"));
    }
}
