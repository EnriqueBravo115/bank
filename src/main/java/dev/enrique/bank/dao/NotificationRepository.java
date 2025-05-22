package dev.enrique.bank.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.enrique.bank.model.Notification;
import dev.enrique.bank.model.User;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserAndReadFalse(User user);

    List<Notification> findByUserOrderByCreatedAtDesc(User user);
}
