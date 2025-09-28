package dev.enrique.bank.client;

import static dev.enrique.bank.commons.constants.PathConstants.ACCOUNT_SERVICE;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import dev.enrique.bank.commons.dto.request.AccountPurchaseRequest;
import dev.enrique.bank.commons.dto.request.AccountServiceRequest;
import dev.enrique.bank.commons.dto.request.AccountTransferRequest;
import dev.enrique.bank.commons.dto.request.AccountWithdrawalRequest;
import dev.enrique.bank.commons.dto.response.MovementResultResponse;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.config.FeignConfiguration;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@FeignClient(value = ACCOUNT_SERVICE, path = "/api/v1/account", configuration = FeignConfiguration.class)
public interface AccountClient {
    @CircuitBreaker(name = ACCOUNT_SERVICE, fallbackMethod = "transferFallback")
    @PostMapping("/transfer")
    MovementResultResponse processTransfer(@RequestBody AccountTransferRequest accountTransferRequest);

    @CircuitBreaker(name = ACCOUNT_SERVICE, fallbackMethod = "purchaseFallback")
    @PostMapping("/purchase")
    MovementResultResponse processPurchase(@RequestBody AccountPurchaseRequest accountPurchaseRequest);

    @CircuitBreaker(name = ACCOUNT_SERVICE, fallbackMethod = "serviceFallback")
    @PostMapping("/service")
    MovementResultResponse processService(@RequestBody AccountServiceRequest accountServiceRequest);

    @CircuitBreaker(name = ACCOUNT_SERVICE, fallbackMethod = "withdrawalFallback")
    @PostMapping("/withdrawal")
    MovementResultResponse processWithdrawal(@RequestBody AccountWithdrawalRequest accountWithdrawalRequest);

    default MovementResultResponse transferFallback(AccountTransferRequest accountTransferRequest,
            Throwable throwable) {
        return new MovementResultResponse(TransactionStatus.FAILED, "Service unavailable");
    }

    default MovementResultResponse purchaseFallback(AccountPurchaseRequest accountPurchaseRequest,
            Throwable throwable) {
        return new MovementResultResponse(TransactionStatus.FAILED, "Service unavailable");
    }

    default MovementResultResponse serviceFallback(AccountServiceRequest accountServiceRequest,
            Throwable throwable) {
        return new MovementResultResponse(TransactionStatus.FAILED, "Service unavailable");
    }

    default MovementResultResponse withdrawalFallback(AccountWithdrawalRequest accountWithdrawalRequest,
            Throwable throwable) {
        return new MovementResultResponse(TransactionStatus.FAILED, "Service unavailable");
    }
}
