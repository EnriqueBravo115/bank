package dev.enrique.bank.service.impl;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.dao.projection.TransactionCommonProjection;
import dev.enrique.bank.service.TransactionSupportService;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class TransactionSupportServiceImpl implements TransactionSupportService {
    private final TransactionRepository transactionRepository;

    @Override
    public Set<String> getAllUniqueTransactionDescriptions(String accountNumber) {
        return transactionRepository.findAllCompletedByAccountNumber(accountNumber, TransactionCommonProjection.class)
                .stream()
                .map(TransactionCommonProjection::getDescription)
                .filter(Objects::nonNull)
                .filter(desc -> !desc.isBlank())
                .flatMap(desc -> Stream.of(desc.split(" ")))
                .filter(word -> !word.isBlank())
                .collect(toSet());
    }

    @Override
    public String getAllTransactionDescriptions(String accountNumber) {
        return transactionRepository.findAllCompletedByAccountNumber(accountNumber, TransactionCommonProjection.class)
                .stream()
                .map(TransactionCommonProjection::getDescription)
                .collect(joining(", "));
    }
}
