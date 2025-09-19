package dev.enrique.bank.client;

import dev.enrique.bank.dto.response.AccountResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import dev.enrique.bank.config.FeignConfiguration;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import static dev.enrique.bank.commons.constants.PathConstants.ACCOUNT_SERVICE;

import java.util.ArrayList;

@FeignClient(value = ACCOUNT_SERVICE, path = "/api/v1/account", configuration = FeignConfiguration.class)
public interface AccountClient {
    @CircuitBreaker(name = ACCOUNT_SERVICE, fallbackMethod = "defaultEmptyArray")
    @GetMapping("")
    AccountResponse validateTransaction(@PathVariable("accountNumber") String accountNumber);

    @CircuitBreaker(name = ACCOUNT_SERVICE, fallbackMethod = "defaultEmptyArray")
    @PostMapping("")
    void updateAccountBalance(@PathVariable("accountNumber") String accountNumber,
            @RequestBody AccountResponse accountResponse);

    default ArrayList<String> defaultEmptyArray(Throwable throwable) {
        return new ArrayList<>();
    }
}
