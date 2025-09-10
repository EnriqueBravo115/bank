package dev.enrique.bank.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.retry.annotation.CircuitBreaker;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import dev.enrique.bank.config.FeignConfiguration;
import static dev.enrique.bank.commons.constants.PathConstants.ACCOUNT_SERVICE;

import java.util.ArrayList;
import java.util.List;

@FeignClient(value = ACCOUNT_SERVICE, path = "/api/v1/account", configuration = FeignConfiguration.class)
public interface AccountClient {
    @CircuitBreaker
    @GetMapping()
    List<String> getAccountId(@PathVariable("text") String text);

    default ArrayList<String> defaultEmptyArray(Throwable throwable) {
        return new ArrayList<>();
    }
}
