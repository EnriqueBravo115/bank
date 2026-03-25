package dev.enrique.bank.client;

import static dev.enrique.bank.commons.constants.PathConstants.ACCOUNT_SERVICE;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.enrique.bank.commons.dto.request.ClientPurchaseRequest;
import dev.enrique.bank.commons.dto.request.ClientServiceRequest;
import dev.enrique.bank.commons.dto.request.ClientTransferRequest;
import dev.enrique.bank.commons.dto.request.ClientWithdrawalRequest;
import dev.enrique.bank.commons.dto.response.MovementResultResponse;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.config.FeignConfiguration;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@FeignClient(value = ACCOUNT_SERVICE, path = "/api/v1/account/movement", configuration = FeignConfiguration.class)
public interface AccountClient {
    @CircuitBreaker(name = ACCOUNT_SERVICE, fallbackMethod = "movementFallback")
    @PostMapping("/transfer")
    MovementResultResponse processTransfer(@RequestBody ClientTransferRequest request);

    @CircuitBreaker(name = ACCOUNT_SERVICE, fallbackMethod = "movementFallback")
    @PostMapping("/purchase")
    MovementResultResponse processPurchase(@RequestBody ClientPurchaseRequest request);

    @CircuitBreaker(name = ACCOUNT_SERVICE, fallbackMethod = "movementFallback")
    @PostMapping("/service")
    MovementResultResponse processService(@RequestBody ClientServiceRequest request);

    @CircuitBreaker(name = ACCOUNT_SERVICE, fallbackMethod = "movementFallback")
    @PostMapping("/withdrawal")
    MovementResultResponse processWithdrawal(@RequestBody ClientWithdrawalRequest request);

    default MovementResultResponse movementFallback(Throwable throwable) {
        if (throwable instanceof FeignException ex) {
            if (ex.status() == 422) {
                try {
                    JsonNode body = new ObjectMapper().readTree(ex.contentUTF8());
                    String message = body.get("message").asText();
                    return new MovementResultResponse(TransactionStatus.DECLINED, message);
                } catch (Exception e) {
                    return new MovementResultResponse(TransactionStatus.DECLINED, "Transaction declined");
                }
            }
            return switch (ex.status()) {
                case 404 -> new MovementResultResponse(TransactionStatus.FAILED, "Account not found");
                default -> new MovementResultResponse(TransactionStatus.FAILED, "Service unavailable");
            };
        }
        return new MovementResultResponse(TransactionStatus.FAILED, "Service unavailable");
    }
}
