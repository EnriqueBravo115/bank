package dev.enrique.bank.service.impl;

import static java.util.stream.Collectors.toList;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import dev.enrique.bank.dao.NotificationRepository;
import dev.enrique.bank.dto.response.NotificationResponse;
import dev.enrique.bank.model.Notification;
import dev.enrique.bank.model.User;
import dev.enrique.bank.service.NotificationService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void createNotification(User user, String message) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setDateSent(LocalDateTime.now());

        notificationRepository.save(notification);

        messagingTemplate.convertAndSendToUser(
                user.getUsername(),
                "/queue/notifications",
                new NotificationResponse(notification.getId(), message, false));
    }

    @Override
    public List<NotificationResponse> getUnreadNotifications(User user) {
        return notificationRepository.findByUserAndReadFalse(user)
                .stream()
                .map(n -> new NotificationResponse(n.getId(), n.getMessage(), n.isRead()))
                .collect(toList());
    }

    @Override
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }
}
