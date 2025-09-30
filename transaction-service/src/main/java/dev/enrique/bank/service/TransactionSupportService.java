package dev.enrique.bank.service;

import java.util.Set;

import org.springframework.stereotype.Service;

@Service
public interface TransactionSupportService {
    Set<String> getAllUniqueTransactionDescriptions(String accountNumber);
    String getAllTransactionDescriptions(String accountNumber);
}
