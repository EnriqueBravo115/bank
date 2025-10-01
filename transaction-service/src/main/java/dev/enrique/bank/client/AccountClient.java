package dev.enrique.bank.client;

import static dev.enrique.bank.commons.constants.PathConstants.ACCOUNT_SERVICE;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import dev.enrique.bank.commons.dto.request.ClientPurchaseRequest;
import dev.enrique.bank.commons.dto.request.ClientServiceRequest;
import dev.enrique.bank.commons.dto.request.ClientTransferRequest;
import dev.enrique.bank.commons.dto.request.ClientWithdrawalRequest;
import dev.enrique.bank.commons.dto.response.MovementResultResponse;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.config.FeignConfiguration;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@FeignClient(value = ACCOUNT_SERVICE, path = "/api/v1/account", configuration = FeignConfiguration.class)
public interface AccountClient {
    @CircuitBreaker(name = ACCOUNT_SERVICE, fallbackMethod = "transferFallback")
    @PostMapping("/transfer")
    MovementResultResponse processTransfer(@RequestBody ClientTransferRequest clientTransferRequest);

    @CircuitBreaker(name = ACCOUNT_SERVICE, fallbackMethod = "purchaseFallback")
    @PostMapping("/purchase")
    MovementResultResponse processPurchase(@RequestBody ClientPurchaseRequest clientPurchaseRequest);

    @CircuitBreaker(name = ACCOUNT_SERVICE, fallbackMethod = "serviceFallback")
    @PostMapping("/service")
    MovementResultResponse processService(@RequestBody ClientServiceRequest clientServiceRequest);

    @CircuitBreaker(name = ACCOUNT_SERVICE, fallbackMethod = "withdrawalFallback")
    @PostMapping("/withdrawal")
    MovementResultResponse processWithdrawal(@RequestBody ClientWithdrawalRequest clientWithdrawalRequest);

    default MovementResultResponse transferFallback(ClientTransferRequest clientTransferRequest,
                                                    Throwable throwable) {
        return new MovementResultResponse(TransactionStatus.FAILED, "Service unavailable");
    }

    default MovementResultResponse purchaseFallback(ClientPurchaseRequest clientPurchaseRequest,
                                                    Throwable throwable) {
        return new MovementResultResponse(TransactionStatus.FAILED, "Service unavailable");
    }

    default MovementResultResponse serviceFallback(ClientServiceRequest clientServiceRequest,
                                                   Throwable throwable) {
        return new MovementResultResponse(TransactionStatus.FAILED, "Service unavailable");
    }

    default MovementResultResponse withdrawalFallback(ClientWithdrawalRequest clientWithdrawalRequest,
                                                      Throwable throwable) {
        return new MovementResultResponse(TransactionStatus.FAILED, "Service unavailable");
    }
}
