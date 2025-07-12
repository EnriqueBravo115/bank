package dev.enrique.bank.mapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import dev.enrique.bank.TransactionServiceTestHelper;
import dev.enrique.bank.commons.constants.TestConstants;
import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.dao.projection.TransactionBasicProjection;
import dev.enrique.bank.dao.projection.TransactionCommonProjection;
import dev.enrique.bank.dao.projection.TransactionDetailedProjection;
import dev.enrique.bank.dto.response.HeaderResponse;
import dev.enrique.bank.dto.response.TransactionBasicResponse;
import dev.enrique.bank.dto.response.TransactionCommonResponse;
import dev.enrique.bank.dto.response.TransactionDetailedResponse;
import dev.enrique.bank.service.TransactionService;

@ExtendWith(MockitoExtension.class)
public class TransactionMapperTest {
    @Mock
    private BasicMapper basicMapper;
    @Mock
    private TransactionService transactionService;
    @InjectMocks
    private TransactionMapper transactionMapper;

    private final PageRequest pageable = PageRequest.of(0, 20);

    @Test
    void getTransactionHistory_ValidAccountId_ReturnsDetailedResponses() {
        List<TransactionDetailedProjection> projections = TransactionServiceTestHelper
                .createTransactionDetailedProjection();
        List<TransactionDetailedResponse> expectedResponses = List.of(new TransactionDetailedResponse());

        when(transactionService.getTransactionHistory(TestConstants.ACCOUNT_ID)).thenReturn(projections);
        when(basicMapper.convertToResponseList(projections, TransactionDetailedResponse.class))
                .thenReturn(expectedResponses);

        List<TransactionDetailedResponse> result = transactionMapper.getTransactionHistory(TestConstants.ACCOUNT_ID);

        assertEquals(expectedResponses, result);
        verify(transactionService).getTransactionHistory(TestConstants.ACCOUNT_ID);
        verify(basicMapper).convertToResponseList(projections, TransactionDetailedResponse.class);
    }

    @Test
    void getAllTransactions_ValidInput_ReturnsPagedResponse() {
        Page<TransactionCommonProjection> page = TransactionServiceTestHelper.createTransactionPageCommonProjections();
        HeaderResponse<TransactionCommonResponse> expectedResponse = new HeaderResponse<>();

        when(transactionService.getAllTransactions(TestConstants.ACCOUNT_ID, pageable)).thenReturn(page);
        when(basicMapper.getHeaderResponse(page, TransactionCommonResponse.class)).thenReturn(expectedResponse);

        HeaderResponse<TransactionCommonResponse> result = transactionMapper
                .getAllTransactions(TestConstants.ACCOUNT_ID, pageable);

        assertEquals(expectedResponse, result);
        verify(transactionService).getAllTransactions(TestConstants.ACCOUNT_ID, pageable);
        verify(basicMapper).getHeaderResponse(page, TransactionCommonResponse.class);
    }

    @Test
    void getTransactionsByYearAndAccount_ValidInput_ReturnsCommonResponses() {
        List<TransactionCommonProjection> projections = TransactionServiceTestHelper
                .createTransactionCommonProjections();
        List<TransactionCommonResponse> expectedResponses = List.of(new TransactionCommonResponse());

        when(transactionService.getTransactionByYearAndAccount(TestConstants.ACCOUNT_ID, TestConstants.YEAR))
                .thenReturn(projections);
        when(basicMapper.convertToResponseList(projections, TransactionCommonResponse.class))
                .thenReturn(expectedResponses);

        List<TransactionCommonResponse> result = transactionMapper
                .getTransactionsByYearAndAccount(TestConstants.ACCOUNT_ID, TestConstants.YEAR);

        assertEquals(expectedResponses, result);
        verify(transactionService).getTransactionByYearAndAccount(TestConstants.ACCOUNT_ID, TestConstants.YEAR);
        verify(basicMapper).convertToResponseList(projections, TransactionCommonResponse.class);
    }

    @Test
    void getAllTransactionsReversals_ValidAccountId_ReturnsBasicResponses() {
        List<TransactionBasicProjection> projections = TransactionServiceTestHelper.createTransactionBasicProjections();
        List<TransactionBasicResponse> expectedResponses = List.of(new TransactionBasicResponse());

        when(transactionService.getAllTransactionsReversals(TestConstants.ACCOUNT_ID)).thenReturn(projections);
        when(basicMapper.convertToResponseList(projections, TransactionBasicResponse.class))
                .thenReturn(expectedResponses);

        List<TransactionBasicResponse> result = transactionMapper.getAllTransactionsReversals(TestConstants.ACCOUNT_ID);

        assertEquals(expectedResponses, result);
        verify(transactionService).getAllTransactionsReversals(TestConstants.ACCOUNT_ID);
        verify(basicMapper).convertToResponseList(projections, TransactionBasicResponse.class);
    }

    @Test
    void getAllTransactionsFromAccounts_ValidAccountIds_ReturnsCommonResponses() {
        List<TransactionCommonProjection> projections = TransactionServiceTestHelper
                .createTransactionCommonProjections();
        List<TransactionCommonResponse> expectedResponses = List.of(new TransactionCommonResponse());

        when(transactionService.getAllTransactionsFromAccounts(TestConstants.ACCOUNT_IDS)).thenReturn(projections);
        when(basicMapper.convertToResponseList(projections, TransactionCommonResponse.class))
                .thenReturn(expectedResponses);

        List<TransactionCommonResponse> result = transactionMapper
                .getAllTransactionsFromAccounts(TestConstants.ACCOUNT_IDS);

        assertEquals(expectedResponses, result);
        verify(transactionService).getAllTransactionsFromAccounts(TestConstants.ACCOUNT_IDS);
        verify(basicMapper).convertToResponseList(projections, TransactionCommonResponse.class);
    }

    //@Test
    //void groupTransactionsByType_ValidAccountId_ReturnsGroupedResponses() {
    //    Map<TransactionType, List<TransactionCommonProjection>> groupedProjections = TransactionServiceTestHelper
    //            .createGroupedTransactions();
    //    Map<TransactionType, List<TransactionCommonResponse>> expectedResponses = new HashMap<>();
    //    expectedResponses.put(TransactionType.TRANSFER, List.of(new TransactionCommonResponse()));

    //    when(transactionService.groupTransactionsByType(TestConstants.ACCOUNT_ID)).thenReturn(groupedProjections);
    //    when(basicMapper.convertToTypedResponseMap(groupedProjections, TransactionCommonResponse.class))
    //            .thenReturn(expectedResponses);

    //    Map<TransactionType, List<TransactionCommonResponse>> result = transactionMapper
    //            .groupTransactionsByType(TestConstants.ACCOUNT_ID);

    //    assertEquals(expectedResponses, result);
    //    verify(transactionService).groupTransactionsByType(TestConstants.ACCOUNT_ID);
    //    verify(basicMapper).convertToTypedResponseMap(groupedProjections, TransactionCommonResponse.class);
    //}

    @Test
    void sumTransactionsByType_ValidAccountId_ReturnsSumMap() {
        Map<TransactionType, BigDecimal> expected = Map.of(TransactionType.TRANSFER, new BigDecimal("100.00"));

        when(transactionService.sumTransactionsByType(TestConstants.ACCOUNT_ID)).thenReturn(expected);

        Map<TransactionType, BigDecimal> result = transactionMapper.sumTransactionsByType(TestConstants.ACCOUNT_ID);

        assertEquals(expected, result);
        verify(transactionService).sumTransactionsByType(TestConstants.ACCOUNT_ID);
    }

    @Test
    void getTransactionYearStatistics_ValidAccountId_ReturnsStatistics() {
        IntSummaryStatistics expected = new IntSummaryStatistics();
        expected.accept(2020);
        expected.accept(2021);

        when(transactionService.getTransactionYearStatistics(TestConstants.ACCOUNT_ID)).thenReturn(expected);

        IntSummaryStatistics result = transactionMapper.getTransactionYearStatistics(TestConstants.ACCOUNT_ID);

        assertEquals(expected, result);
        verify(transactionService).getTransactionYearStatistics(TestConstants.ACCOUNT_ID);
    }

//    @Test
//    void partitionTransactionsByAmount_ValidInput_ReturnsPartitionedResponses() {
//        Map<Boolean, List<TransactionBasicProjection>> partitionedProjections = TransactionServiceTestHelper
//                .createPartitionedTransactions();
//        Map<Boolean, List<TransactionBasicResponse>> expectedResponses = new HashMap<>();
//        expectedResponses.put(true, List.of(new TransactionBasicResponse()));
//
//        when(transactionService.partitionTransactionsByAmount(TestConstants.ACCOUNT_ID, TestConstants.AMOUNT))
//                .thenReturn(partitionedProjections);
//        when(basicMapper.convertToBooleanKeyResponseMap(partitionedProjections, TransactionBasicResponse.class))
//                .thenReturn(expectedResponses);
//
//        Map<Boolean, List<TransactionBasicResponse>> result = transactionMapper
//                .partitionTransactionsByAmount(TestConstants.ACCOUNT_ID, TestConstants.AMOUNT);
//
//        assertEquals(expectedResponses, result);
//        verify(transactionService).partitionTransactionsByAmount(TestConstants.ACCOUNT_ID, TestConstants.AMOUNT);
//        verify(basicMapper).convertToBooleanKeyResponseMap(partitionedProjections, TransactionBasicResponse.class);
//    }

    @Test
    void getTransactionTypeSummary_ValidAccountId_ReturnsSummary() {
        Map<TransactionType, String> expected = Map.of(TransactionType.TRANSFER, "Count: 2, Total: $300.00");

        when(transactionService.getTransactionTypeSummary(TestConstants.ACCOUNT_ID)).thenReturn(expected);

        Map<TransactionType, String> result = transactionMapper.getTransactionTypeSummary(TestConstants.ACCOUNT_ID);

        assertEquals(expected, result);
        verify(transactionService).getTransactionTypeSummary(TestConstants.ACCOUNT_ID);
    }

    @Test
    void calculateTotalTransactionAmount_ValidAccountId_ReturnsTotal() {
        BigDecimal expected = new BigDecimal("1000.00");

        when(transactionService.calculateTotalTransactionAmount(TestConstants.ACCOUNT_ID)).thenReturn(expected);

        BigDecimal result = transactionMapper.calculateTotalTransactionAmount(TestConstants.ACCOUNT_ID);

        assertEquals(expected, result);
        verify(transactionService).calculateTotalTransactionAmount(TestConstants.ACCOUNT_ID);
    }

    @Test
    void calculateTotalAmountByType_ValidInput_ReturnsTotal() {
        BigDecimal expected = new BigDecimal("500.00");

        when(transactionService.calculateTotalAmountByType(TestConstants.ACCOUNT_ID, TransactionType.TRANSFER))
                .thenReturn(expected);

        BigDecimal result = transactionMapper.calculateTotalAmountByType(TestConstants.ACCOUNT_ID,
                TransactionType.TRANSFER);

        assertEquals(expected, result);
        verify(transactionService).calculateTotalAmountByType(TestConstants.ACCOUNT_ID, TransactionType.TRANSFER);
    }

    @Test
    void calculateTransferFee_ValidInput_ReturnsFee() {
        BigDecimal expected = new BigDecimal("30.00");

        when(transactionService.calculateTransferFee(TestConstants.AMOUNT, Currency.MXN)).thenReturn(expected);

        BigDecimal result = transactionMapper.calculateTransferFee(TestConstants.AMOUNT, Currency.MXN);

        assertEquals(expected, result);
        verify(transactionService).calculateTransferFee(TestConstants.AMOUNT, Currency.MXN);
    }

    @Test
    void hasSufficientFunds_ValidInput_ReturnsBoolean() {
        when(transactionService.hasSufficientFunds(TestConstants.ACCOUNT_ID, TestConstants.AMOUNT)).thenReturn(true);

        Boolean result = transactionMapper.hasSufficientFunds(TestConstants.ACCOUNT_ID, TestConstants.AMOUNT);

        assertTrue(result);
        verify(transactionService).hasSufficientFunds(TestConstants.ACCOUNT_ID, TestConstants.AMOUNT);
    }

    @Test
    void getTransferLimit_ValidAccountId_ReturnsLimit() {
        BigDecimal expected = new BigDecimal("2000.00");

        when(transactionService.getTransferLimit(TestConstants.ACCOUNT_ID)).thenReturn(expected);

        BigDecimal result = transactionMapper.getTransferLimit(TestConstants.ACCOUNT_ID);

        assertEquals(expected, result);
        verify(transactionService).getTransferLimit(TestConstants.ACCOUNT_ID);
    }

    @Test
    void getAllUniqueTransactionDescriptions_ValidAccountId_ReturnsUniqueWords() {
        Set<String> expected = Set.of("Salary", "Bonus");

        when(transactionService.getAllUniqueTransactionDescriptions(TestConstants.ACCOUNT_ID)).thenReturn(expected);

        Set<String> result = transactionMapper.getAllUniqueTransactionDescriptions(TestConstants.ACCOUNT_ID);

        assertEquals(expected, result);
        verify(transactionService).getAllUniqueTransactionDescriptions(TestConstants.ACCOUNT_ID);
    }

    @Test
    void getAllTransactionDescriptions_ValidAccountId_ReturnsConcatenatedString() {
        String expected = "Salary, Bonus";

        when(transactionService.getAllTransactionDescriptions(TestConstants.ACCOUNT_ID)).thenReturn(expected);

        String result = transactionMapper.getAllTransactionDescriptions(TestConstants.ACCOUNT_ID);

        assertEquals(expected, result);
        verify(transactionService).getAllTransactionDescriptions(TestConstants.ACCOUNT_ID);
    }

    @Test
    void getFormattedAverageBalance_ValidAccountIds_ReturnsFormattedString() {
        String expected = "$1,500.00";

        when(transactionService.getFormattedAverageBalance(TestConstants.ACCOUNT_IDS)).thenReturn(expected);

        String result = transactionMapper.getFormattedAverageBalance(TestConstants.ACCOUNT_IDS);

        assertEquals(expected, result);
        verify(transactionService).getFormattedAverageBalance(TestConstants.ACCOUNT_IDS);
    }

    @Test
    void findMaxTransaction_ValidAccountId_ReturnsOptionalResponse() {
        TransactionBasicProjection projection = TransactionServiceTestHelper.createTransactionBasicProjections().get(0);
        TransactionBasicResponse expectedResponse = new TransactionBasicResponse();

        when(transactionService.findMaxTransaction(TestConstants.ACCOUNT_ID)).thenReturn(Optional.of(projection));
        when(basicMapper.convertOptionalResponse(Optional.of(projection), TransactionBasicResponse.class))
                .thenReturn(Optional.of(expectedResponse));

        Optional<TransactionBasicResponse> result = transactionMapper.findMaxTransaction(TestConstants.ACCOUNT_ID);

        assertTrue(result.isPresent());
        assertEquals(expectedResponse, result.get());
        verify(transactionService).findMaxTransaction(TestConstants.ACCOUNT_ID);
        verify(basicMapper).convertOptionalResponse(Optional.of(projection), TransactionBasicResponse.class);
    }

    @Test
    void getAverageDaysBetweenTransactions_ValidAccountId_ReturnsAverage() {
        double expected = 2.5;

        when(transactionService.getAverageDaysBetweenTransactions(TestConstants.ACCOUNT_ID)).thenReturn(expected);

        double result = transactionMapper.getAverageDaysBetweenTransactions(TestConstants.ACCOUNT_ID);

        assertEquals(expected, result, 0.01);
        verify(transactionService).getAverageDaysBetweenTransactions(TestConstants.ACCOUNT_ID);
    }
}
