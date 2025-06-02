package dev.enrique.bank.mapper;

import java.math.BigDecimal;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

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
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TransactionMapper {
    private final BasicMapper basicMapper;
    private final TransactionService transactionService;

    public List<TransactionDetailedResponse> getTransactionHistory(Long accountId) {
        List<TransactionDetailedProjection> projections = transactionService.getTransactionHistory(accountId);
        return basicMapper.convertToResponseList(projections, TransactionDetailedResponse.class);
    }

    public HeaderResponse<TransactionCommonResponse> getAllTransactions(Long accountId, Pageable pageable) {
        Page<TransactionCommonProjection> page = transactionService.getAllTransactions(accountId, pageable);
        return basicMapper.getHeaderResponse(page, TransactionCommonResponse.class);
    }

    public List<TransactionCommonResponse> getTransactionsByYearAndAccount(Long accountId, Integer year) {
        List<TransactionCommonProjection> projections = transactionService
                .getTransactionByYearAndAccount(accountId, year);
        return basicMapper.convertToResponseList(projections, TransactionCommonResponse.class);
    }

    public List<TransactionBasicResponse> getAllTransactionsReversals(Long accountId) {
        List<TransactionBasicProjection> projections = transactionService.getAllTransactionsReversals(accountId);
        return basicMapper.convertToResponseList(projections, TransactionBasicResponse.class);
    }

    public List<TransactionCommonResponse> getAllTransactionsFromAccounts(List<Long> accountIds) {
        List<TransactionCommonProjection> projections = transactionService.getAllTransactionsFromAccounts(accountIds);
        return basicMapper.convertToResponseList(projections, TransactionCommonResponse.class);
    }

    public Map<TransactionType, List<TransactionCommonResponse>> groupTransactionsByType(Long accountId) {
        Map<TransactionType, List<TransactionCommonProjection>> groupedProjections = transactionService
                .groupTransactionsByType(accountId);
        return basicMapper.convertToTypedResponseMap(groupedProjections, TransactionCommonResponse.class);
    }

    public Map<TransactionType, BigDecimal> sumTransactionsByType(Long accountId) {
        return transactionService.sumTransactionsByType(accountId);
    }

    public IntSummaryStatistics getTransactionYearStatistics(Long accountId) {
        return transactionService.getTransactionYearStatistics(accountId);
    }

    public Map<Boolean, List<TransactionBasicResponse>> partitionTransactionsByAmount(Long accountId,
            BigDecimal amount) {
        Map<Boolean, List<TransactionBasicProjection>> partitionedProjections = transactionService
                .partitionTransactionsByAmount(accountId, amount);
        return basicMapper.convertToBooleanKeyResponseMap(partitionedProjections, TransactionBasicResponse.class);
    }

    public Map<TransactionType, String> getTransactionTypeSummary(Long accountId) {
        return transactionService.getTransactionTypeSummary(accountId);
    }

    public BigDecimal calculateTotalTransactionAmount(Long accountId) {
        return transactionService.calculateTotalTransactionAmount(accountId);
    }

    public BigDecimal calculateTotalAmountByType(Long accountId, TransactionType type) {
        return transactionService.calculateTotalAmountByType(accountId, type);
    }

    public BigDecimal calculateTransferFee(BigDecimal amount, Currency currency) {
        return transactionService.calculateTransferFee(amount, currency);
    }

    public Boolean hasSufficientFunds(Long accountId, BigDecimal amount) {
        return transactionService.hasSufficientFunds(accountId, amount);
    }

    public BigDecimal getTransferLimit(Long accountId) {
        return transactionService.getTransferLimit(accountId);
    }

    public Set<String> getAllUniqueTransactionDescriptions(Long accountId) {
        return transactionService.getAllUniqueTransactionDescriptions(accountId);
    }

    public String getAllTransactionDescriptions(Long accountId) {
        return transactionService.getAllTransactionDescriptions(accountId);
    }

    public String getFormattedAverageBalance(List<Long> accountIds) {
        return transactionService.getFormattedAverageBalance(accountIds);
    }

    public Optional<TransactionBasicResponse> findMaxTransaction(Long accountId) {
        Optional<TransactionBasicProjection> projection = transactionService.findMaxTransaction(accountId);
        return basicMapper.convertOptionalResponse(projection, TransactionBasicResponse.class);
    }

    public double getAverageDaysBetweenTransactions(Long accountId) {
        return transactionService.getAverageDaysBetweenTransactions(accountId);
    }
}
