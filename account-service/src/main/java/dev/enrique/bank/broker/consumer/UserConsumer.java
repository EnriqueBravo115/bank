package dev.enrique.bank.broker.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.messaging.handler.annotation.Header;

import dev.enrique.bank.broker.event.CreateAccountEvent;
import dev.enrique.bank.service.AccountService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserConsumer {
    private final AccountService accountService;

    @KafkaListener(topics = "user-service.create-user", groupId = "${spring.kafka.consumer.group-id}")
    public void createAccount(CreateAccountEvent createAccountEvent, @Header("auth-user-id") String authId) {
        accountService.createAccount(createAccountEvent, authId);
    }
}
