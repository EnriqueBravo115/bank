package dev.enrique.bank.client;

import static dev.enrique.bank.commons.constants.Headers.IDEMPOTENCY_KEY;
import static dev.enrique.bank.commons.constants.PathConstants.ACCOUNT_SERVICE;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.enrique.bank.commons.dto.request.ClientPurchaseRequest;
import dev.enrique.bank.commons.dto.request.ClientServiceRequest;
import dev.enrique.bank.commons.dto.request.ClientTransferRequest;
import dev.enrique.bank.commons.dto.request.ClientWithdrawalRequest;
import dev.enrique.bank.commons.dto.response.AccountResponse;
import dev.enrique.bank.commons.dto.response.MovementResultResponse;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.config.FeignConfiguration;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

/**
 * Calls account-service with transaction-service's own token (client credentials, role SERVICE).
 * Every movement carries the transaction code as Idempotency-Key, so it can be retried safely.
 */
@FeignClient(value = ACCOUNT_SERVICE, path = "/api/v1/account", configuration = FeignConfiguration.class)
public interface AccountClient {
    String AWAITING_CONFIRMATION = "Awaiting confirmation from account-service";

    @GetMapping("/get-by-account-number/{accountNumber}")
    AccountResponse getByAccountNumber(@PathVariable String accountNumber);

    @GetMapping("/get-by-clabe/{clabe}")
    AccountResponse getByClabe(@PathVariable String clabe);

    @GetMapping("/get-by-card-number/{cardNumber}")
    AccountResponse getByCardNumber(@PathVariable String cardNumber);

    @CircuitBreaker(name = ACCOUNT_SERVICE, fallbackMethod = "transferFallback")
    @PostMapping("/movement/transfer")
    MovementResultResponse processTransfer(@RequestHeader(IDEMPOTENCY_KEY) String idempotencyKey,
            @RequestBody ClientTransferRequest request);

    @CircuitBreaker(name = ACCOUNT_SERVICE, fallbackMethod = "purchaseFallback")
    @PostMapping("/movement/purchase")
    MovementResultResponse processPurchase(@RequestHeader(IDEMPOTENCY_KEY) String idempotencyKey,
            @RequestBody ClientPurchaseRequest request);

    @CircuitBreaker(name = ACCOUNT_SERVICE, fallbackMethod = "serviceFallback")
    @PostMapping("/movement/service")
    MovementResultResponse processService(@RequestHeader(IDEMPOTENCY_KEY) String idempotencyKey,
            @RequestBody ClientServiceRequest request);

    @CircuitBreaker(name = ACCOUNT_SERVICE, fallbackMethod = "withdrawalFallback")
    @PostMapping("/movement/withdrawal")
    MovementResultResponse processWithdrawal(@RequestHeader(IDEMPOTENCY_KEY) String idempotencyKey,
            @RequestBody ClientWithdrawalRequest request);

    @CircuitBreaker(name = ACCOUNT_SERVICE, fallbackMethod = "reverseFallback")
    @PostMapping("/movement/{idempotencyKey}/reverse")
    MovementResultResponse reverse(@PathVariable String idempotencyKey);

    // Resilience4j matches a fallback by the parameters of the method plus the exception.
    default MovementResultResponse transferFallback(String idempotencyKey, ClientTransferRequest request,
            Throwable throwable) {
        return toResult(throwable);
    }

    default MovementResultResponse purchaseFallback(String idempotencyKey, ClientPurchaseRequest request,
            Throwable throwable) {
        return toResult(throwable);
    }

    default MovementResultResponse serviceFallback(String idempotencyKey, ClientServiceRequest request,
            Throwable throwable) {
        return toResult(throwable);
    }

    default MovementResultResponse withdrawalFallback(String idempotencyKey, ClientWithdrawalRequest request,
            Throwable throwable) {
        return toResult(throwable);
    }

    default MovementResultResponse reverseFallback(String idempotencyKey, Throwable throwable) {
        return toResult(throwable);
    }

    /**
     * Only answers that prove the money did not move are final (DECLINED/FAILED). A timeout, a 5xx,
     * a 409 (same key in flight) or an open circuit leave it unknown: PENDING, retried later.
     */
    static MovementResultResponse toResult(Throwable throwable) {
        if (!(throwable instanceof FeignException ex)) {
            return new MovementResultResponse(TransactionStatus.PENDING, AWAITING_CONFIRMATION);
        }
        return switch (ex.status()) {
            case 422 -> new MovementResultResponse(TransactionStatus.DECLINED, messageOf(ex));
            case 404 -> new MovementResultResponse(TransactionStatus.FAILED, "Account not found");
            case 400 -> new MovementResultResponse(TransactionStatus.FAILED, "Invalid movement request");
            case 401, 403 -> new MovementResultResponse(TransactionStatus.FAILED, "Unauthorized operation");
            default -> new MovementResultResponse(TransactionStatus.PENDING, AWAITING_CONFIRMATION);
        };
    }

    private static String messageOf(FeignException ex) {
        try {
            JsonNode body = new ObjectMapper().readTree(ex.contentUTF8());
            return body.get("message").asText();
        } catch (Exception e) {
            return "Transaction declined";
        }
    }
}
