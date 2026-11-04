package dev.enrique.bank.broker.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Same class name and package in account-service: JsonSerializer sends it in the __TypeId__ header.
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserEmailChangedEvent {
    private String email;
}
