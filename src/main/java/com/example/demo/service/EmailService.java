package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendEmail(String to, String subject, String text) {
        if (!StringUtils.hasText(to) || !StringUtils.hasText(subject) || !StringUtils.hasText(text)) {
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            message.setFrom("maihuychuong78@gmail.com"); // Thay đổi theo email của bạn

            mailSender.send(message);

        } catch (Exception e) {
            // Handle silently
        }
    }

    public void sendAlertResolvedEmail(String adminEmail, String alertMessage, String alertId, String resolverName) {
        String subject = "Cảnh báo đã được giải quyết - ID: " + alertId;
        String body = String.format(
                "Xin chào,\n\n" +
                        "Cảnh báo sau đây đã được giải quyết:\n\n" +
                        "ID Cảnh báo: %s\n" +
                        "Nội dung: %s\n" +
                        "Được giải quyết bởi: %s\n" +
                        "Thời gian: %s\n\n" +
                        "Trân trọng,\n" +
                        "Hệ thống quản lý cảnh báo",
                alertId,
                alertMessage,
                resolverName,
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        );

        sendEmail(adminEmail, subject, body);
    }
}