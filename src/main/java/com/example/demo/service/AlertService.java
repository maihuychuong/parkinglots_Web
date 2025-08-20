package com.example.demo.service;

import com.example.demo.entity.Alert;
import com.example.demo.entity.User;
import com.example.demo.model.enums.AlertType;
import com.example.demo.model.enums.Role;
import com.example.demo.repository.AlertRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AlertService {
    private final AlertRepository alertRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final ShiftService shiftService;
    private final EmailService emailService;

    public List<Alert> getAllAlerts(String typeStr, String resolvedStr) {
        try {
            AlertType type = StringUtils.hasText(typeStr) ? AlertType.valueOf(typeStr) : null;
            Boolean resolved = StringUtils.hasText(resolvedStr) ? Boolean.valueOf(resolvedStr) : null;

            if (type != null && resolved != null) {
                return alertRepository.findByTypeAndResolvedOrderByCreatedAtDesc(type, resolved);
            } else if (type != null) {
                return alertRepository.findByTypeOrderByCreatedAtDesc(type);
            } else if (resolved != null) {
                return alertRepository.findByResolvedOrderByCreatedAtDesc(resolved);
            } else {
                return alertRepository.findAllByOrderByCreatedAtDesc();
            }
        } catch (IllegalArgumentException e) {
            return List.of(); // Return empty list for invalid type
        }
    }

    public Alert createAlertAndNotify(Alert alert) {
        if (alert == null || !StringUtils.hasText(alert.getMessage())) {
            throw new IllegalArgumentException("Alert and its message must not be null or empty.");
        }

        if (alert.getCreatedAt() == null) {
            alert.setCreatedAt(LocalDateTime.now());
        }

        Alert savedAlert = alertRepository.save(alert);

        List<User> admins = userRepository.findByRole(Role.ADMIN);
        Instant alertTime = savedAlert.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant();
        List<User> staffOnShift = shiftService.findStaffsByTimeRange(alertTime, alertTime);

        Set<User> recipients = new HashSet<>();
        recipients.addAll(admins);
        recipients.addAll(staffOnShift);

        for (User user : recipients) {
            notificationService.sendNotification(
                    user.getUsername(),
                    "Cảnh báo mới: " + savedAlert.getType().name(),
                    savedAlert.getMessage() + " (ID: " + savedAlert.getId() + ")",
                    savedAlert.getId() // Pass alertId
            );
        }

        return savedAlert;
    }

    public Alert markAlertAsResolved(String alertId, String resolverUsername) {
        if (!StringUtils.hasText(alertId) || !StringUtils.hasText(resolverUsername)) {
            throw new IllegalArgumentException("Alert ID and resolver username must not be empty.");
        }

        Alert alert = alertRepository.findById(alertId).orElse(null);
        if (alert == null || alert.getResolved()) {
            return null;
        }

        alert.setResolved(true);
        alert.setResolvedAt(LocalDateTime.now());
        User resolver = userRepository.findByUsername(resolverUsername).orElse(null);
        if (resolver != null) {
            alert.setResolvedBy(resolver);
        }

        Alert updatedAlert = alertRepository.save(alert);

        // Gửi notification và email cho admin
        List<User> admins = userRepository.findByRole(Role.ADMIN);

        for (User admin : admins) {
            try {
                // Gửi notification trong app
                notificationService.sendNotification(
                        admin.getUsername(),
                        "Cảnh báo đã được giải quyết",
                        "Cảnh báo \"" + alert.getMessage() + "\" (ID: " + alert.getId() + ") đã được nhân viên " + resolverUsername + " xử lý.",
                        alert.getId()
                );

                // Gửi email (nếu admin có email)
                if (StringUtils.hasText(admin.getEmail())) {
                    emailService.sendAlertResolvedEmail(
                            admin.getEmail(),
                            alert.getMessage(),
                            alert.getId(),
                            resolverUsername
                    );
                }

            } catch (Exception e) {
                // Handle silently
            }
        }

        return updatedAlert;
    }

    public Alert getAlertById(String id) {
        if (!StringUtils.hasText(id)) {
            throw new IllegalArgumentException("Alert ID must not be empty.");
        }
        return alertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert not found."));
    }

    public long countUnresolvedAlerts() {
        return alertRepository.countByResolvedFalse();
    }
}