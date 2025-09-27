package dev.enrique.bank.commons.util;

import dev.enrique.bank.commons.dto.request.BaseRequest;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.model.Transaction;

public interface TransactionFactory<T extends BaseRequest> {
    Transaction build(T request, String transactionCode, TransactionStatus transactionStatus, String reason);
    Class<T> getRequestType();
}
