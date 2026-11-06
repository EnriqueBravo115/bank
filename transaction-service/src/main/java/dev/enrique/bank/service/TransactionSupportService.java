package dev.enrique.bank.service;

import java.util.Set;

public interface TransactionSupportService {
    Set<String> getAllUniqueTransactionDescriptions(String accountNumber);
    String getAllTransactionDescriptions(String accountNumber);
}
