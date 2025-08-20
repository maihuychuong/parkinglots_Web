package com.example.demo.repository;

import com.example.demo.entity.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByRecipientOrderByCreatedAtDesc(String recipient);
    long countByRecipientAndReadFalse(String recipient);
}