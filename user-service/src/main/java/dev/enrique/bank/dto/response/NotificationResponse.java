package dev.enrique.bank.dto.response;

import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.Getter;

@Getter
@Setter
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private String message;
    private boolean read;
}
