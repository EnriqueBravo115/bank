package dev.enrique.bank.service;

import java.util.List;

import dev.enrique.bank.dto.response.NotificationResponse;
import dev.enrique.bank.model.User;

public interface NotificationService {
    public void createNotification(User user, String message);

    public List<NotificationResponse> getUnreadNotifications(User user);

    public void markAsRead(Long notificationId);
}
