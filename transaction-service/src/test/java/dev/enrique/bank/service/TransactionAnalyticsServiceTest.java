package dev.enrique.bank.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.enrique.bank.commons.util.BasicMapper;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.service.impl.TransactionAnalyticsServiceImpl;

@ExtendWith(MockitoExtension.class)
public class TransactionAnalyticsServiceTest {
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private BasicMapper basicMapper;
    @InjectMocks
    private TransactionAnalyticsServiceImpl transactionAnalyticsServiceImpl;

    @Test
    void groupTransactionsByType_shouldReturnGroupedTransactions() {
    }
}
