package dev.enrique.bank.mapper;

import java.math.BigDecimal;
import java.util.List;

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

    public List<TransactionDetailedResponse> getTransactionHistoryAsResponse(Long accountId) {
        List<TransactionDetailedProjection> projections = transactionService.getTransactionHistory(accountId);
        return basicMapper.convertToResponseList(projections, TransactionDetailedResponse.class);
    }

    public List<TransactionCommonResponse> getTransactionsByYearAndAccount(Long accountId, Integer year) {
        List<TransactionCommonProjection> projections = transactionService
                .getTransactionByYearAndAccount(accountId, year);
        return basicMapper.convertToResponseList(projections, TransactionCommonResponse.class);
    }

    public HeaderResponse<TransactionCommonResponse> getAllTransactions(Long accountId, Pageable pageable) {
        Page<TransactionCommonProjection> page = transactionService.getAllTransactions(accountId, pageable);
        return basicMapper.getHeaderResponse(page, TransactionCommonResponse.class);
    }

    public List<TransactionBasicResponse> getAllTransactionsReversals(Long accountId) {
        List<TransactionBasicProjection> projections = transactionService.getAllTransactionsReversals(accountId);
        return basicMapper.convertToResponseList(projections, TransactionBasicResponse.class);
    }

    public BigDecimal calculateTotalTransactionAmount(Long accountId) {
        return transactionService.calculateTotalTransactionAmount(accountId);
    }

    public BigDecimal calculateTotalAmountByType(Long accountId, TransactionType type) {
        return transactionService.calculateTotalAmountByType(accountId, type);
    }

    public Boolean hasSufficientFunds(Long accountId, BigDecimal amount) {
        return transactionService.hasSufficientFunds(accountId, amount);
    }

    public BigDecimal calculateTransferFee(BigDecimal amount, Currency currency) {
        return transactionService.calculateTransferFee(amount, currency);
    }
}
