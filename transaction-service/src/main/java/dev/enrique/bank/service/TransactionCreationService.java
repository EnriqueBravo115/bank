package dev.enrique.bank.service;

import dev.enrique.bank.commons.dto.request.PurchaseRequest;
import dev.enrique.bank.commons.dto.request.ServiceRequest;
import dev.enrique.bank.commons.dto.request.TransferRequest;
import dev.enrique.bank.commons.dto.request.WithdrawalRequest;
import dev.enrique.bank.commons.dto.response.TransactionResultResponse;

public interface TransactionCreationService {
    TransactionResultResponse transfer(TransferRequest request);

    TransactionResultResponse purchase(PurchaseRequest request);

    TransactionResultResponse servicePayment(ServiceRequest request);

    TransactionResultResponse withdrawal(WithdrawalRequest request);

    TransactionResultResponse cancel(String transactionCode);

    TransactionResultResponse reverse(String transactionCode);
}
