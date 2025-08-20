package com.example.demo.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    private String id;
    private String title;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;
    private String recipient;
    private String alertId; // New field for linking to an alert
}