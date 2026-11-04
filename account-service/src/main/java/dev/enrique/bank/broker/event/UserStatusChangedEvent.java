package dev.enrique.bank.broker.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Published by user-service: same class name and package there (JsonDeserializer uses the __TypeId__ header).
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserStatusChangedEvent {
    private boolean active;
}
