package dev.enrique.bank.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import dev.enrique.bank.commons.dto.response.HeaderResponse;
import dev.enrique.bank.commons.dto.response.TransactionCommonResponse;
import dev.enrique.bank.commons.dto.response.TransactionDetailedResponse;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.util.BasicMapper;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.dao.projection.TransactionCommonProjection;
import dev.enrique.bank.dao.projection.TransactionDetailedProjection;
import dev.enrique.bank.service.TransactionQueryService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionQueryServiceImpl implements TransactionQueryService {
    private final TransactionRepository transactionRepository;
    private final BasicMapper basicMapper;

    @Override
    public List<TransactionDetailedResponse> getTransactionHistory(String accountNumber, TransactionStatus status) {
        List<TransactionDetailedProjection> projections = transactionRepository
                .findAllByAccountNumberAndStatus(accountNumber, status, TransactionDetailedProjection.class);

        return basicMapper.convertToResponseList(projections, TransactionDetailedResponse.class);
    }

    @Override
    public HeaderResponse<TransactionCommonResponse> getAllTransactions(String accountNumber, TransactionStatus status,
            Pageable pageable) {
        Page<TransactionCommonProjection> page = transactionRepository
                .findAllPageableByAccountNumberAndStatus(accountNumber, status, pageable);

        return basicMapper.getHeaderResponse(page, TransactionCommonResponse.class);
    }

    @Override
    public List<TransactionDetailedResponse> getTransactionsByYear(String accountNumber, TransactionStatus status, Integer year) {
        List<TransactionDetailedProjection> projections = transactionRepository
                .findAllByAccountNumberAndYear(accountNumber, year, status);

        return basicMapper.convertToResponseList(projections, TransactionDetailedResponse.class);
    }

    // This function must be executed with "ADMIN" credentials
    @Override
    public List<TransactionCommonResponse> getAllTransactionsFromAccounts(List<String> accountNumbers) {
        List<TransactionCommonProjection> projections = transactionRepository
                .findAllCompletedByAccountIdsIn(accountNumbers);

        return basicMapper.convertToResponseList(projections, TransactionCommonResponse.class);
    }

    @Override
    public Optional<TransactionCommonResponse> findMaxTransaction(String accountNumber) {
        Optional<TransactionCommonProjection> projection = transactionRepository
                .findAllCompletedByAccountNumber(accountNumber, TransactionCommonProjection.class)
                .stream()
                .reduce((t1, t2) -> t1.getAmount().compareTo(t2.getAmount()) > 0 ? t1 : t2);

        return basicMapper.convertOptionalResponse(projection, TransactionCommonResponse.class);
    }
}
