package dev.enrique.bank.service.impl;

import static java.util.stream.Collectors.averagingDouble;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.model.Transaction;
import dev.enrique.bank.service.TransactionSupportService;
import dev.enrique.bank.service.util.AccountHelper;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class TransactionSupportServiceImpl implements TransactionSupportService {
    private final TransactionRepository transactionRepository;
    private final AccountHelper accountHelper;

    @Override
    public Set<String> getAllUniqueTransactionDescriptions(Long accountId) {
        return transactionRepository.findAllByAccountId(accountId).stream()
                .map(Transaction::getDescription)
                .filter(Objects::nonNull)
                .filter(desc -> !desc.isBlank())
                .flatMap(desc -> Stream.of(desc.split(" ")))
                .filter(word -> !word.isBlank())
                .collect(toSet());
    }

    @Override
    public String getAllTransactionDescriptions(Long accountId) {
        return transactionRepository.findAllByAccountId(accountId).stream()
                .map(Transaction::getDescription)
                .collect(joining(", "));
    }

    @Override
    public String getFormattedAverageBalance(List<Long> accountIds) {
        return accountIds.stream()
                .map(accountHelper::getAccountById)
                .collect(collectingAndThen(
                        averagingDouble(a -> a.getBalance().doubleValue()),
                        avg -> String.format("$%,.2f", avg)));
    }
}
