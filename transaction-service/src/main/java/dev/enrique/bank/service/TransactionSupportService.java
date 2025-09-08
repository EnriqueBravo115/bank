package dev.enrique.bank.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

@Service
public interface TransactionSupportService {
    Set<String> getAllUniqueTransactionDescriptions(Long accountId);
    String getAllTransactionDescriptions(Long accountId);
    String getFormattedAverageBalance(List<Long> accountIds);
}
