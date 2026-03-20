package dev.enrique.bank.broker.event;

import dev.enrique.bank.commons.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateAccountEvent {
    String clabe;
    Currency currency;
}
