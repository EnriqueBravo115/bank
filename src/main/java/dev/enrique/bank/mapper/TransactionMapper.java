package dev.enrique.bank.mapper;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import dev.enrique.bank.dao.projection.TransactionCommonProjection;
import dev.enrique.bank.dao.projection.TransactionDetailedProjection;
import dev.enrique.bank.dto.response.HeaderResponse;
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

    public HeaderResponse<TransactionCommonResponse> getAllTransactions(Pageable pageable) {
        Page<TransactionCommonProjection> page = transactionService.getAllTransactions(pageable);
        return basicMapper.getHeaderResponse(page, TransactionCommonResponse.class);
    }
}
