package dev.enrique.bank.broker.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import dev.enrique.bank.broker.ProducerUtil;
import dev.enrique.bank.broker.event.CreateAccountEvent;
import dev.enrique.bank.commons.enums.AccountType;
import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.util.BasicMapper;
import dev.enrique.bank.model.User;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AccountProducer {
    private final KafkaTemplate<String, CreateAccountEvent> kafkaTemplate;
    private final BasicMapper basicMapper;

    public void sendCreateAccountEvent(User user, String authId) {
        CreateAccountEvent createAccountEvent = basicMapper.convertToResponse(user, CreateAccountEvent.class);
        createAccountEvent.setCurrency(Currency.MX);
        createAccountEvent.setAccountType(AccountType.SAVING);
        kafkaTemplate.send(ProducerUtil.authHeaderWrapper("user-service.account-requested", createAccountEvent, authId));
    }
}
