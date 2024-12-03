package dev.enrique.bank.pojo.entity;

import java.time.LocalDateTime;

import dev.enrique.bank.commons.enums.NotificationStatus;
import dev.enrique.bank.commons.enums.NotificationType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String message;

    private LocalDateTime dateSent;

    private NotificationStatus status;

    private NotificationType type;
}
