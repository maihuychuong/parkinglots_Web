package com.example.demo.service;

import com.example.demo.entity.Notification;
import com.example.demo.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public void sendNotification(String recipient, String title, String message, String alertId) {
        if (!StringUtils.hasText(recipient) || !StringUtils.hasText(title) || !StringUtils.hasText(message)) {
            throw new IllegalArgumentException("Recipient, title, and message must not be empty.");
        }

        Notification notification = Notification.builder()
                .recipient(recipient)
                .title(title)
                .message(message)
                .read(false)
                .createdAt(LocalDateTime.now())
                .alertId(alertId) // Set alertId
                .build();

        notificationRepository.save(notification);
    }

    public List<Notification> getNotificationsForUser(String recipient) {
        if (!StringUtils.hasText(recipient)) {
            throw new IllegalArgumentException("Recipient must not be empty.");
        }
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(recipient);
    }

    public void markAsRead(String id, String username) {
        Optional<Notification> optionalNotification = notificationRepository.findById(id);
        if (optionalNotification.isPresent()) {
            Notification notification = optionalNotification.get();
            if (!notification.getRecipient().equals(username)) {
                throw new SecurityException("User is not authorized to mark this notification as read.");
            }
            if (!notification.isRead()) {
                notification.setRead(true);
                notificationRepository.save(notification);
            }
        }
    }

    public long countUnreadNotifications(String recipient) {
        if (!StringUtils.hasText(recipient)) {
            throw new IllegalArgumentException("Recipient must not be empty.");
        }
        return notificationRepository.countByRecipientAndReadFalse(recipient);
    }

    public Optional<Notification> getNotificationById(String id) {
        if (!StringUtils.hasText(id)) {
            throw new IllegalArgumentException("Notification ID must not be empty.");
        }
        return notificationRepository.findById(id);
    }
}