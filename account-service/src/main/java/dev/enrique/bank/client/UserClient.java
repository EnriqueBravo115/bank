package dev.enrique.bank.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import dev.enrique.bank.config.FeignConfiguration;
import dev.enrique.bank.dto.response.UserResponse;

@FeignClient(value = "user-service", path = "/api/v1/user", configuration = FeignConfiguration.class)
public interface UserClient {
    @GetMapping
    UserResponse getUserById(@PathVariable("userId") Long userId);

    @GetMapping
    boolean userExists(@PathVariable("userId") Long userId);
}
