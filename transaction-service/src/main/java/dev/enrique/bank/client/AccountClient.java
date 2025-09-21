package dev.enrique.bank.client;

import static dev.enrique.bank.commons.constants.PathConstants.ACCOUNT_SERVICE;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.config.FeignConfiguration;
import dev.enrique.bank.dto.request.AccountPurchaseRequest;
import dev.enrique.bank.dto.request.AccountTransferRequest;
import dev.enrique.bank.dto.response.MovementResultResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@FeignClient(value = ACCOUNT_SERVICE, path = "/api/v1/account", configuration = FeignConfiguration.class)
public interface AccountClient {
    @CircuitBreaker(name = ACCOUNT_SERVICE, fallbackMethod = "transferFallback")
    @PostMapping("/transfer")
    MovementResultResponse processTransfer(@RequestBody AccountTransferRequest accountTransferRequest);

    @CircuitBreaker(name = ACCOUNT_SERVICE, fallbackMethod = "transferFallback")
    @PostMapping("/purchase")
    MovementResultResponse processPurchase(@RequestBody AccountPurchaseRequest accountPurchaseRequest);

    default MovementResultResponse transferFallback(AccountTransferRequest accountTransferRequest,
            Throwable throwable) {
        return new MovementResultResponse(TransactionStatus.FAILED, "Service unavailable");
    }
}
