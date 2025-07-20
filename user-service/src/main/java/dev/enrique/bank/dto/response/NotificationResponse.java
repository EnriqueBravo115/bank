package dev.enrique.bank.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;

@Gettter
@Setter
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private String message;
    private boolean read;
}
