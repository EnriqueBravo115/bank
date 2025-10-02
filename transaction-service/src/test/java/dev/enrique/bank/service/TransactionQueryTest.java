//package dev.enrique.bank.service;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//import java.util.List;
//
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import dev.enrique.bank.TransactionServiceTestHelper;
//import dev.enrique.bank.constants.TestConstants;
//import dev.enrique.bank.dao.TransactionRepository;
//import dev.enrique.bank.dao.projection.TransactionDetailedProjection;
//import dev.enrique.bank.dto.response.TransactionDetailedResponse;
//import dev.enrique.bank.service.impl.TransactionQueryServiceImpl;
//import dev.enrique.bank.service.util.BasicMapper;
//
//@ExtendWith(MockitoExtension.class)
//public class TransactionQueryTest {
//    @Mock
//    private TransactionRepository transactionRepository;
//    @Mock
//    private BasicMapper basicMapper;
//    @InjectMocks
//    private TransactionQueryServiceImpl transactionQueryService;
//
//    @Test
//    void getTransactionHistory_ReturnsResponse() {
//        List<TransactionDetailedProjection> projections = TransactionServiceTestHelper
//                .createTransactionDetailedProjection();
//
//        List<TransactionDetailedResponse> responses = TransactionServiceTestHelper
//                .createTransactionDetailedResponse();
//
//        when(transactionRepository.findCompletedByAccountId(
//                TestConstants.ACCOUNT_ID, TransactionDetailedProjection.class))
//                .thenReturn(projections);
//
//        when(basicMapper.convertToResponseList(projections, TransactionDetailedResponse.class))
//                .thenReturn(responses);
//
//        List<TransactionDetailedResponse> result = transactionQueryService
//                .getTransactionHistory(TestConstants.ACCOUNT_ID);
//
//        assertNotNull(result);
//        assertEquals(responses.size(), result.size());
//        assertEquals(responses.get(0).getTransactionNumber(), result.get(0).getTransactionNumber());
//
//        verify(transactionRepository, times(1))
//                .findCompletedByAccountId(TestConstants.ACCOUNT_ID, TransactionDetailedProjection.class);
//        verify(basicMapper, times(1))
//                .convertToResponseList(projections, TransactionDetailedResponse.class);
//    }
//}
