package dev.enrique.bank.service.impl;

import static java.util.stream.Collectors.reducing;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.dao.projection.TransactionCommonProjection;
import dev.enrique.bank.dao.projection.TransactionDetailedProjection;
import dev.enrique.bank.dto.response.HeaderResponse;
import dev.enrique.bank.dto.response.TransactionCommonResponse;
import dev.enrique.bank.dto.response.TransactionDetailedResponse;
import dev.enrique.bank.service.TransactionQueryService;
import dev.enrique.bank.commons.util.BasicMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionQueryServiceImpl implements TransactionQueryService {
    private final TransactionRepository transactionRepository;
    private final BasicMapper basicMapper;

    @Override
    public List<TransactionDetailedResponse> getTransactionHistory(Long accountId) {
        List<TransactionDetailedProjection> projections = transactionRepository
                .findCompletedByAccountId(accountId, TransactionDetailedProjection.class);

        return basicMapper.convertToResponseList(projections, TransactionDetailedResponse.class);
    }

    @Override
    public HeaderResponse<TransactionCommonResponse> getAllTransactions(Long accountId, Pageable pageable) {
        Page<TransactionCommonProjection> page = transactionRepository.findCompletedByAccountId(accountId, pageable);
        return basicMapper.getHeaderResponse(page, TransactionCommonResponse.class);
    }

    @Override
    public List<TransactionDetailedResponse> getTransactionByYearAndAccount(Long accountId, Integer year) {
        List<TransactionDetailedProjection> projections = transactionRepository.findByAccountIdAndYear(accountId, year);
        return basicMapper.convertToResponseList(projections, TransactionDetailedResponse.class);
    }

    @Override
    public List<TransactionCommonResponse> getAllTransactionsReversals(Long accountId) {
        List<TransactionCommonProjection> projections = transactionRepository.findReversalsByAccountId(accountId);
        return basicMapper.convertToResponseList(projections, TransactionCommonResponse.class);
    }

    // Esta funcion tiene que ser ejecutadas con permisos de ADMIN
    @Override
    public List<TransactionCommonResponse> getAllTransactionsFromAccounts(List<Long> accountIds) {
        accountIds.forEach(accountHelper::validateAccountId);
        List<TransactionCommonProjection> projections = transactionRepository.findCompletedByAccountIdsIn(accountIds);
        return basicMapper.convertToResponseList(projections, TransactionCommonResponse.class);
    }

    @Override
    public Optional<TransactionCommonResponse> findMaxTransaction(Long accountId) {
        accountHelper.validateAccountId(accountId);
        Optional<TransactionCommonProjection> projection = transactionRepository
                .findCompletedByAccountId(accountId, TransactionCommonProjection.class).stream()
                .reduce((t1, t2) -> t1.getAmount().compareTo(t2.getAmount()) > 0 ? t1 : t2);
        return basicMapper.convertOptionalResponse(projection, TransactionCommonResponse.class);
    }
}
