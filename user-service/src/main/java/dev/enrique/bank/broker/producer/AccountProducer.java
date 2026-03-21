package dev.enrique.bank.broker.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import dev.enrique.bank.broker.ProducerUtil;
import dev.enrique.bank.broker.event.CreateAccountEvent;
import dev.enrique.bank.commons.util.BasicMapper;
import dev.enrique.bank.model.User;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AccountProducer {
    private final KafkaTemplate<String, CreateAccountEvent> kafkaTemplate;
    private final BasicMapper basicMapper;

    public void sendCreateAccountEvent(User user, String authId) {
        CreateAccountEvent createAccountEvent = basicMapper.toCreateAccountEvent(user);
        kafkaTemplate.send(ProducerUtil.authHeaderWrapper("user-service.create-user", createAccountEvent, authId));
    }
}
