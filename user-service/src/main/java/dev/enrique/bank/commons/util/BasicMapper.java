package dev.enrique.bank.commons.util;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import dev.enrique.bank.broker.event.CreateAccountEvent;
import dev.enrique.bank.commons.enums.AccountType;
import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class BasicMapper {
    private final ModelMapper mapper;

    public <T, S> S convertToResponse(T data, Class<S> type) {
        return mapper.map(data, type);
    }

    public CreateAccountEvent toCreateAccountEvent(User user) {
        CreateAccountEvent createAccountEvent = convertToResponse(user, CreateAccountEvent.class);
        createAccountEvent.setCurrency(Currency.MX);
        createAccountEvent.setAccountType(AccountType.SAVING);

        return createAccountEvent;
    }
}
